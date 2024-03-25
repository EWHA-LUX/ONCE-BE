package ewha.lux.once.domain.card.service;

import ewha.lux.once.global.common.CustomException;
import ewha.lux.once.global.common.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
//@RequiredArgsConstructor
public class CrawlingService {
    private static final Logger LOG = LoggerFactory.getLogger(CrawlingService.class);

    // 매주 월요일 00:00 카드 혜택 크롤링
//    @Scheduled(cron = "0 0 0 ? * 1")
    public void cardCrawling() throws IOException, InterruptedException {
        String[] cardCompanyList = {"Kookmin", "Hyundai", "Samsung", "Shinhan", "Lotte", "Hana"};
        for (String cardCompany : cardCompanyList){
            crawling(cardCompany);
        }
        // 카드 혜택 요약 진행
    }

    private static void crawling(String cardCompany) throws IOException, InterruptedException {
        LOG.info(cardCompany+" 크롤링 시작");
        executeFile(cardCompany+"/credit.py");
        executeInsertData(cardCompany,"Credit");
        executeFile(cardCompany+"/debit.py");
        executeInsertData(cardCompany,"Debit");
    }
    private static void executeFile(String path) throws IOException, InterruptedException {
//            ResourceLoader loader = null;
//            val file = loader.getResource("classpath:/example.txt").file;
//            InputStream inputStream = new ClassPathResource("folder/resourceFile.dat").getInputStream();
//            File file =File.createTempFile("resourceFile",".py");
//            try {
//                FileUtils.copyInputStreamToFile(inputStream, file);
//            } finally {
//                IOUtils.closeQuietly(inputStream);
//            }

//            Resource resources = ResourcePatternUtils
//                    .getResourcePatternResolver(new DefaultResourceLoader())
//                    .getResource("classpath:crawling/Kookmin/credit.py");
////                    .getResource("classpath*:crawling/"+path);
////            for( Resource re : resources){
////                LOG.info(String.valueOf(re));
////                LOG.info(String.valueOf(re.exists()));
////                LOG.info(String.valueOf(re.isFile()));
////            }
//
//            LOG.info(String.valueOf(resources.exists()));
//            LOG.info(String.valueOf(resources.isFile()));
//            LOG.info(String.valueOf(resources.getURI()));
//            InputStream inputStream = resources.getInputStream();
//            //------------------------------------
// 코드 자체를 출력해보기(성공)
            // InputStream으로부터 데이터를 읽어올 BufferedReader 생성
            String filePath = "/crawling/"+path;

// 파일을 열어서 입력 스트림 생성
            FileInputStream fileInputStream = new FileInputStream(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream));
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder stringBuilder = new StringBuilder();

//// BufferedReader를 사용하여 데이터를 읽어와 StringBuilder에 추가
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

// StringBuilder에 저장된 데이터를 출력
            LOG.info(stringBuilder.toString());
            //------------------------------------
//            InputStream inputStream = new ClassPathResource("crawling/"+path).getInputStream();
            LOG.info("=======================================");

//            File file =File.createTempFile("temp",".py");
////            FileUtils.copyInputStreamToFile(inputStream, file);
////            IOUtils.closeQuietly(inputStream);
//            try {
//                FileUtils.copyInputStreamToFile(inputStream, file);
//                System.out.println("임시 파일에 데이터 복사 완료");
//            } catch (IOException e) {
//                System.out.println("임시 파일에 데이터 복사 중 오류 발생: " + e.getMessage());
//                e.printStackTrace();
//            } finally {
//                IOUtils.closeQuietly(inputStream);
//            }
//            file.setExecutable(true);
//            if (file.exists()) {
//                System.out.println("임시 파일이 정상적으로 생성되었습니다.");
//                System.out.println(file.isFile());
//                System.out.println(file.canExecute());
//                System.out.println(file.getAbsolutePath());
//            } else {
//                System.out.println("임시 파일 생성에 문제가 있습니다.");
//            }

            // --------------------------2--------------------------
//            FileOutputStream local_file=  new FileOutputStream("temp.py");
//            IOUtils.copy(inputStream, local_file);
//            local_file.close();
//            if (file.exists()) {
//                System.out.println("임시 파일이 정상적으로 생성되었습니다.");
//            } else {
//                System.out.println("임시 파일 생성에 문제가 있습니다.");
//            }

            //=====================================================================
            System.out.println("1");
            ProcessBuilder pb = new ProcessBuilder("python3", "/crawling/"+path);
            System.out.println("2");
            pb.redirectErrorStream(true);
            System.out.println("3");
            Process p = pb.start();
//            List<String> results = readProcessOutput(process.getInputStream());
            System.out.println("4");
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

            List<String> results;

            results = br.lines().collect(Collectors.toList());

            for (String result : results) {
                LOG.info(result);
            }
            p.waitFor();

    }
    private static void executeInsertData(String firstInput, String secondInput) throws InterruptedException, IOException {
            ProcessBuilder pb = new ProcessBuilder("python", "./crawling/DatabaseInsert.py",firstInput,secondInput);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

            List<String> results;

            results = br.lines().collect(Collectors.toList());

            for (String result : results) {
                LOG.info(result);
            }
            p.waitFor();

    }
}
