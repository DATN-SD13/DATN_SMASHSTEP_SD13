package com.smashstep.datn.promotion.service;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Local CID images; never sends internal URLs or downloads arbitrary remote images. */
@Component
public class OrderEmailImages {
    @Value("${smashstep.product-images.directory:uploads/products}")
    private String directory="uploads/products";
    public record InlineImage(String cid,byte[] bytes,String contentType) {}
    public record Prepared(EmailService.OrderEmail order,List<InlineImage> images) {}

    public Prepared prepare(EmailService.OrderEmail order){
        var images=new ArrayList<InlineImage>();
        addLogo(images,"ss-brand","mail/smashstep.png");
        if(isGhn(order.shippingCarrier()))addLogo(images,"ss-ghn","mail/ghn.png");
        var mapped=new ArrayList<EmailService.OrderEmailLine>();
        var loaded=new HashMap<String,String>();
        long budget=0;
        for(var line:order.lines()==null?List.<EmailService.OrderEmailLine>of():order.lines()){
            String cid=loaded.get(line.image());
            if(cid==null && line.image()!=null && !loaded.containsKey(line.image())){
                var image=product(line.image(),"ss-item-"+mapped.size());
                if(image!=null && budget+image.bytes().length<=8*1024*1024){
                    images.add(image);budget+=image.bytes().length;cid="cid:"+image.cid();
                }
                loaded.put(line.image(),cid);
            }
            mapped.add(new EmailService.OrderEmailLine(line.name(),line.variant(),line.quantity(),line.unitPrice(),line.total(),cid));
        }
        var result=new EmailService.OrderEmail(order.code(),order.customer(),order.receiver(),order.phone(),order.address(),order.method(),order.status(),
                order.paid(),order.subtotal(),order.discount(),order.shipping(),order.total(),mapped,order.url(),order.voucherApplied(),order.shippingCarrier());
        return new Prepared(result,List.copyOf(images));
    }
    public static boolean isGhn(String carrier){
        String value=Objects.toString(carrier,"").trim().toUpperCase(Locale.ROOT);
        return value.equals("GHN") || value.contains("GIAO HÀNG NHANH") || value.contains("GIAO HANG NHANH");
    }
    private void addLogo(List<InlineImage> images,String cid,String path){
        try(var input=new ClassPathResource(path).getInputStream()){
            byte[] bytes=input.readNBytes(1024*1024);
            // The store's GHN asset has a large white canvas; trim that canvas for email.
            if("ss-ghn".equals(cid)){
                var original=ImageIO.read(new ByteArrayInputStream(bytes));
                if(original!=null){
                    int left=original.getWidth(),top=original.getHeight(),right=-1,bottom=-1;
                    for(int y=0;y<original.getHeight();y++)for(int x=0;x<original.getWidth();x++){
                        int p=original.getRGB(x,y);
                        if((p>>>24)>0 && (((p>>16)&255)<245 || ((p>>8)&255)<245 || (p&255)<245)){
                            left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
                        }
                    }
                    if(right>=left){var output=new ByteArrayOutputStream();
                        ImageIO.write(original.getSubimage(left,top,right-left+1,bottom-top+1),"png",output);bytes=output.toByteArray();}
                }
            }
            if(bytes.length>0)images.add(new InlineImage(cid,bytes,"image/png"));
        }catch(IOException ignored){/* Packaged logos are optional for old installations. */}
    }
    private InlineImage product(String url,String cid){
        if(!url.matches("/uploads/products/[a-zA-Z0-9][a-zA-Z0-9._-]{0,180}\\.(?i:jpg|jpeg|png|webp)"))return null;
        try{
            var root=Path.of(directory).toRealPath();
            var file=root.resolve(url.substring("/uploads/products/".length())).toRealPath();
            if(!file.startsWith(root) || !Files.isRegularFile(file) || Files.size(file)>5*1024*1024)return null;
            try(var input=ImageIO.createImageInputStream(file.toFile())){
                var readers=ImageIO.getImageReaders(input);
                if(!readers.hasNext()){
                    // Gmail supports WebP; retain it when the JVM has no WebP decoder.
                    if(url.toLowerCase(Locale.ROOT).endsWith(".webp") && Files.size(file)<=512*1024)
                        return new InlineImage(cid,Files.readAllBytes(file),"image/webp");
                    return null;
                }
                var reader=readers.next();
                try{
                    reader.setInput(input);
                    int width=reader.getWidth(0),height=reader.getHeight(0);
                    if(width<=0 || height<=0 || (long)width*height>20_000_000)return null;
                    var original=reader.read(0);
                    double ratio=Math.min(1,240.0/Math.max(width,height));
                    var thumb=new BufferedImage(Math.max(1,(int)(width*ratio)),Math.max(1,(int)(height*ratio)),BufferedImage.TYPE_INT_RGB);
                    var g=thumb.createGraphics();
                    try{g.setColor(Color.WHITE);g.fillRect(0,0,thumb.getWidth(),thumb.getHeight());
                        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        g.drawImage(original,0,0,thumb.getWidth(),thumb.getHeight(),null);
                    }finally{g.dispose();}
                    var bytes=new ByteArrayOutputStream();ImageIO.write(thumb,"jpg",bytes);
                    return new InlineImage(cid,bytes.toByteArray(),"image/jpeg");
                }finally{reader.dispose();}
            }
        }catch(IOException|RuntimeException ignored){return null;}
    }
}
