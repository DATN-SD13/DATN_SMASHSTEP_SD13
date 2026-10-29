package com.smashstep.datn.statistics.service;

import com.smashstep.datn.common.exception.AppException;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Calendar-only email delivery; persistent local claims, no additional database tables. */
@Service
public class StatisticsReportSchedule {
    private static final System.Logger LOG=System.getLogger(StatisticsReportSchedule.class.getName());
    @Value("${smashstep.statistics-reports.directory:${user.home}/.smashstep/statistics-reports}")
    private String directory=Path.of(System.getProperty("user.home"),".smashstep","statistics-reports").toString();
    private Clock clock=Clock.system(StatisticsReportService.ZONE);
    private final StatisticsReportService reports;
    private final AtomicBoolean working=new AtomicBoolean();
    private final ExecutorService worker=Executors.newSingleThreadExecutor(task->{var t=new Thread(task,"statistics-report-mail");t.setDaemon(true);return t;});
    public StatisticsReportSchedule(StatisticsReportService reports){this.reports=reports;}
    public record Settings(String email,boolean monthlyEnabled,boolean yearlyEnabled,YearMonth monthlySince,Integer yearlySince,boolean dailyEnabled,LocalDate dailySince){
        public Settings(String email,boolean monthly,boolean yearly,YearMonth month,Integer year){this(email,monthly,yearly,month,year,false,null);}
    }
    public record View(String email,boolean monthlyEnabled,boolean yearlyEnabled,String timezone,String lastMonthlySent,String lastYearlySent,boolean dailyEnabled,String lastDailySent){}
    public record Period(String kind,String key,LocalDate start,LocalDate end,ZonedDateTime due){}

    public synchronized View view(){
        try{var s=read();return new View(s.email(),s.monthlyEnabled(),s.yearlyEnabled(),StatisticsReportService.ZONE.getId(),lastSent(s.email(),"month"),lastSent(s.email(),"year"),s.dailyEnabled(),lastSent(s.email(),"day"));}
        catch(IOException e){throw new IllegalStateException("Không đọc được cấu hình lịch gửi báo cáo",e);}
    }
    public synchronized View save(String email,boolean monthly,boolean yearly){return save(email,monthly,yearly,false);}
    public synchronized View save(String email,boolean monthly,boolean yearly,boolean daily){
        email=Objects.toString(email,"").trim().toLowerCase(Locale.ROOT);
        if((monthly||yearly||daily) && (email.length()>254 || !email.matches("^[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+$")))throw AppException.badRequest("Nhập email hợp lệ trước khi bật lịch gửi tự động");
        if(email.length()>254)throw AppException.badRequest("Email không được dài quá 254 ký tự");
        try{
            var previous=read();var now=LocalDate.now(clock);boolean same=email.equalsIgnoreCase(previous.email());
            var settings=new Settings(email,monthly,yearly,
                    monthly && same && previous.monthlyEnabled()?previous.monthlySince():YearMonth.from(now),
                    yearly && same && previous.yearlyEnabled()?previous.yearlySince():now.getYear(),daily,
                    daily && same && previous.dailyEnabled()?previous.dailySince():now);
            var p=new Properties();p.setProperty("email",settings.email());p.setProperty("monthly",String.valueOf(monthly));p.setProperty("yearly",String.valueOf(yearly));
            p.setProperty("monthlySince",settings.monthlySince().toString());p.setProperty("yearlySince",settings.yearlySince().toString());
            p.setProperty("daily",String.valueOf(daily));p.setProperty("dailySince",settings.dailySince().toString());
            writeAtomic(root().resolve("settings.properties"),p);return view();
        }catch(IOException e){throw new IllegalStateException("Không lưu được lịch gửi báo cáo",e);}
    }
    private Path root()throws IOException{
        var configured=Path.of(directory);
        // Relative custom paths are anchored to the user home, not the shell/IDE launch folder.
        var root=(configured.isAbsolute()?configured:Path.of(System.getProperty("user.home")).resolve(configured)).toAbsolutePath().normalize();
        Files.createDirectories(root);return root;
    }
    private Settings read()throws IOException{
        var now=LocalDate.now(clock);var file=root().resolve("settings.properties");
        if(!Files.exists(file))return new Settings("",false,false,YearMonth.from(now),now.getYear());
        var p=new Properties();try(var input=Files.newInputStream(file)){p.load(input);}
        try{return new Settings(p.getProperty("email",""),Boolean.parseBoolean(p.getProperty("monthly","false")),Boolean.parseBoolean(p.getProperty("yearly","false")),
                YearMonth.parse(p.getProperty("monthlySince",YearMonth.from(now).toString())),Integer.valueOf(p.getProperty("yearlySince",String.valueOf(now.getYear()))),Boolean.parseBoolean(p.getProperty("daily","false")),LocalDate.parse(p.getProperty("dailySince",now.toString())));}
        catch(RuntimeException e){throw new IOException("Cấu hình lịch báo cáo không hợp lệ",e);}
    }
    public static List<Period> duePeriods(Settings s,ZonedDateTime instant){
        var now=instant.withZoneSameInstant(StatisticsReportService.ZONE);var result=new ArrayList<Period>();
        if(s.email()==null || s.email().isBlank() || now.getHour()!=0)return result;
        var today=now.toLocalDate();
        if(now.getMinute()==5){
            var day=today.minusDays(1);
            if(s.dailyEnabled() && s.dailySince()!=null && !day.isBefore(s.dailySince()))
                result.add(new Period("day",day.toString(),day,day,today.atTime(0,5).atZone(StatisticsReportService.ZONE)));
            var month=YearMonth.from(today).minusMonths(1);
            if(today.getDayOfMonth()==1 && s.monthlyEnabled() && s.monthlySince()!=null && !month.isBefore(s.monthlySince()))
                result.add(new Period("month",month.toString(),month.atDay(1),month.atEndOfMonth(),today.atTime(0,5).atZone(StatisticsReportService.ZONE)));
        }
        int year=today.getYear()-1;
        if(now.getMinute()==10 && today.getMonthValue()==1 && today.getDayOfMonth()==1 && s.yearlyEnabled() && s.yearlySince()!=null && year>=s.yearlySince())
            result.add(new Period("year",String.valueOf(year),LocalDate.of(year,1,1),LocalDate.of(year,12,31),today.atTime(0,10).atZone(StatisticsReportService.ZONE)));
        return result;
    }
    @Scheduled(cron="0 5,10 0 * * *",zone="Asia/Ho_Chi_Minh")
    public void tick(){
        if(!working.compareAndSet(false,true))return;
        try{worker.execute(()->{try{processDue();}catch(RuntimeException e){LOG.log(System.Logger.Level.WARNING,"Không xử lý được lịch báo cáo; kiểm tra quyền truy cập thư mục cấu hình.");}finally{working.set(false);}});}
        catch(RejectedExecutionException e){working.set(false);}
    }
    public void processDue(){
        try{
            var settings=read();
            for(var period:duePeriods(settings,ZonedDateTime.now(clock))){
                if(!settings.equals(read()))return;
                var base=period.kind()+"-"+period.key()+"-"+recipientKey(settings.email());
                // Honor successful receipts written by the previous file-based version.
                if(Files.exists(root().resolve(base+".sent")))continue;
                var attempt=root().resolve(base+".attempt");var receipt=new Properties();
                receipt.setProperty("kind",period.kind());receipt.setProperty("period",period.key());
                receipt.setProperty("status","CLAIMED");receipt.setProperty("attemptedAt",clock.instant().toString());
                if(!claim(attempt,receipt))continue;
                try{reports.send(settings.email(),period.start(),period.end());}
                catch(RuntimeException e){
                    receipt.setProperty("status","FAILED");writeAtomic(attempt,receipt);
                    LOG.log(System.Logger.Level.WARNING,"Gửi báo cáo không thành công; không tự thử lại kỳ này để tránh thư lặp.");continue;
                }
                receipt.setProperty("status","SENT");receipt.setProperty("sentAt",clock.instant().toString());writeAtomic(attempt,receipt);
            }
        }catch(IOException e){throw new IllegalStateException("Không truy cập được thư mục lịch báo cáo; không gửi thêm thư",e);}
    }
    // CREATE_NEW is atomic across processes using the SAME local directory. Claim before SMTP.
    private static boolean claim(Path file,Properties receipt)throws IOException{
        var bytes=new ByteArrayOutputStream();receipt.store(bytes,"SmashStep email attempt");
        try(var channel=FileChannel.open(file,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){
            var buffer=ByteBuffer.wrap(bytes.toByteArray());while(buffer.hasRemaining())channel.write(buffer);channel.force(true);return true;
        }catch(FileAlreadyExistsException alreadyClaimed){return false;}
    }
    private String lastSent(String email,String kind)throws IOException{
        if(email==null || email.isBlank())return null;
        String suffix="-"+recipientKey(email),last=null;
        try(var files=Files.list(root())){
            for(var file:files.filter(p->{var name=p.getFileName().toString();return name.startsWith(kind+"-") && (name.endsWith(suffix+".sent") || name.endsWith(suffix+".attempt"));}).toList()){
                var p=new Properties();try(var input=Files.newInputStream(file)){p.load(input);}
                if(file.toString().endsWith(".attempt") && !"SENT".equals(p.getProperty("status")))continue;
                String at=p.getProperty("sentAt");if(at!=null && (last==null || at.compareTo(last)>0))last=at;
            }
        }return last;
    }
    private static String recipientKey(String email){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(email.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private static void writeAtomic(Path file,Properties props)throws IOException{
        var temporary=Files.createTempFile(file.getParent(),"report-",".tmp");
        try{try(var output=Files.newOutputStream(temporary)){props.store(output,"SmashStep statistics reports");}
            try{Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temporary);}
    }
    @PreDestroy public void close(){worker.shutdown();}
}
