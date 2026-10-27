package com.smashstep.datn.statistics.service;

import com.smashstep.datn.common.exception.AppException;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Persistent local settings and per-period receipts; no database migration is needed. */
@Service
public class StatisticsReportSchedule {
    private static final System.Logger LOG=System.getLogger(StatisticsReportSchedule.class.getName());
    @Value("${smashstep.statistics-reports.directory:private/statistics-reports}")
    private String directory="private/statistics-reports";
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
        email=Objects.toString(email,"").trim();
        if((monthly||yearly||daily) && (email.length()>254 || !email.matches("^[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+$")))throw AppException.badRequest("Nhập email hợp lệ trước khi bật lịch gửi tự động");
        try{
            var previous=read();var now=LocalDate.now(clock);boolean same=email.equalsIgnoreCase(previous.email());
            var settings=new Settings(email,monthly,yearly,
                    monthly && same && previous.monthlyEnabled()?previous.monthlySince():YearMonth.from(now),
                    yearly && same && previous.yearlyEnabled()?previous.yearlySince():now.getYear(),daily,
                    daily && same && previous.dailyEnabled()?previous.dailySince():now);
            var p=new Properties();p.setProperty("email",settings.email());p.setProperty("monthly",String.valueOf(monthly));p.setProperty("yearly",String.valueOf(yearly));
            p.setProperty("monthlySince",settings.monthlySince().toString());p.setProperty("yearlySince",settings.yearlySince().toString());
            p.setProperty("daily",String.valueOf(daily));p.setProperty("dailySince",settings.dailySince().toString());
            writeAtomic(root().resolve("settings.properties"),p);
            return view();
        }catch(IOException e){throw new IllegalStateException("Không lưu được lịch gửi báo cáo",e);}
    }
    private Path root()throws IOException{var root=Path.of(directory).toAbsolutePath().normalize();Files.createDirectories(root);return root;}
    private Settings read()throws IOException{
        var now=LocalDate.now(clock);var file=root().resolve("settings.properties");
        if(!Files.exists(file))return new Settings("",false,false,YearMonth.from(now),now.getYear());
        var p=new Properties();try(var input=Files.newInputStream(file)){p.load(input);}
        try{return new Settings(p.getProperty("email",""),Boolean.parseBoolean(p.getProperty("monthly","false")),Boolean.parseBoolean(p.getProperty("yearly","false")),
                YearMonth.parse(p.getProperty("monthlySince",YearMonth.from(now).toString())),Integer.valueOf(p.getProperty("yearlySince",String.valueOf(now.getYear()))),Boolean.parseBoolean(p.getProperty("daily","false")),LocalDate.parse(p.getProperty("dailySince",now.toString())));}
        catch(RuntimeException e){throw new IOException("Cấu hình lịch báo cáo không hợp lệ",e);}
    }
    public static List<Period> duePeriods(Settings settings,ZonedDateTime instant){
        var now=instant.withZoneSameInstant(StatisticsReportService.ZONE);var result=new ArrayList<Period>();
        if(settings.email()==null || settings.email().isBlank())return result;
        if(settings.dailyEnabled() && settings.dailySince()!=null){
            var first=settings.dailySince();
            if(first.getYear()<2000 || first.getYear()>2100)throw new IllegalArgumentException("Invalid daily schedule year");
            for(var day=first;day.isBefore(now.toLocalDate());day=day.plusDays(1)){
                var due=day.plusDays(1).atTime(0,5).atZone(StatisticsReportService.ZONE);
                if(!now.isBefore(due))result.add(new Period("day",day.toString(),day,day,due));
            }
        }
        if(settings.monthlyEnabled() && settings.monthlySince()!=null){
            var start=settings.monthlySince();
            if(start.getYear()<2000 || start.getYear()>2100)throw new IllegalArgumentException("Invalid schedule year");
            for(var month=start;month.isBefore(YearMonth.from(now));month=month.plusMonths(1)){
                var due=month.plusMonths(1).atDay(1).atTime(0,5).atZone(StatisticsReportService.ZONE);
                if(!now.isBefore(due))result.add(new Period("month",month.toString(),month.atDay(1),month.atEndOfMonth(),due));
            }
        }
        if(settings.yearlyEnabled() && settings.yearlySince()!=null){
            int first=settings.yearlySince();if(first<2000 || first>2100)throw new IllegalArgumentException("Invalid schedule year");
            for(int year=first;year<now.getYear();year++){
                var due=LocalDate.of(year+1,1,1).atTime(0,10).atZone(StatisticsReportService.ZONE);
                if(!now.isBefore(due))result.add(new Period("year",String.valueOf(year),LocalDate.of(year,1,1),LocalDate.of(year,12,31),due));
            }
        }
        result.sort(Comparator.comparing(Period::due));return result;
    }
    @Scheduled(fixedDelay=60_000,initialDelay=30_000)
    public void tick(){
        if(!working.compareAndSet(false,true))return;
        try{worker.execute(()->{try{processDue();}catch(RuntimeException e){LOG.log(System.Logger.Level.WARNING,"Chưa xử lý được lịch báo cáo; sẽ thử lại.");}finally{working.set(false);}});}
        catch(RejectedExecutionException e){working.set(false);}
    }
    public void processDue(){
        try{
            var settings=read();int attempted=0;
            for(var period:duePeriods(settings,ZonedDateTime.now(clock))){
                String base=period.kind()+"-"+period.key()+"-"+recipientKey(settings.email());
                var sent=root().resolve(base+".sent");if(Files.exists(sent))continue;
                if(attempted++>=4)break;
                try(var channel=FileChannel.open(root().resolve(base+".lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE)){
                    FileLock lock;
                    try{lock=channel.tryLock();}catch(OverlappingFileLockException e){continue;}
                    if(lock==null)continue;
                    try(lock){
                        if(Files.exists(sent))continue;
                        var current=read();
                        if(!settings.equals(current))return;
                        reports.send(settings.email(),period.start(),period.end());
                        var receipt=new Properties();receipt.setProperty("sentAt",Instant.now(clock).toString());receipt.setProperty("period",period.key());receipt.setProperty("kind",period.kind());
                        writeAtomic(sent,receipt);
                    }
                }catch(RuntimeException e){LOG.log(System.Logger.Level.WARNING,"Gửi báo cáo chưa thành công; sẽ thử lại ở lượt tiếp theo.");}
            }
        }catch(IOException e){throw new IllegalStateException("Không truy cập được thư mục lịch báo cáo",e);}
    }
    private String lastSent(String email,String kind)throws IOException{
        if(email==null || email.isBlank())return null;
        String suffix="-"+recipientKey(email)+".sent",last=null;
        try(var files=Files.list(root())){
            for(var file:files.filter(p->p.getFileName().toString().startsWith(kind+"-") && p.getFileName().toString().endsWith(suffix)).toList()){
                var p=new Properties();try(var input=Files.newInputStream(file)){p.load(input);}String at=p.getProperty("sentAt");
                if(at!=null && (last==null || at.compareTo(last)>0))last=at;
            }
        }
        return last;
    }
    private static String recipientKey(String email){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(email.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)));}
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
