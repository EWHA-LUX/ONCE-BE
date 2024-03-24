package ewha.lux.once.domain.card.service;

import ewha.lux.once.global.common.CustomException;
import ewha.lux.once.global.common.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;


@Service
@Slf4j
@RequiredArgsConstructor
public class CrawlingService {
    private static final Logger LOG = LoggerFactory.getLogger(CrawlingService.class);

    // 매주 월요일 00:00 카드 혜택 크롤링
//    @Scheduled(cron = "0 0 0 ? * 1")
    public void cardCrawling() throws CustomException {
        String[] cardCompanyList = {"Kookmin", "Hyundai", "Samsung", "Shinhan", "Lotte", "Hana"};
        for (String cardCompany : cardCompanyList){
            crawling(cardCompany);
        }
        // 카드 혜택 요약 진행
    }

    private static void crawling(String cardCompany) throws CustomException{
        LOG.info(cardCompany+" 크롤링 시작");
        executeFile(cardCompany+"/credit.py");
        executeInsertData(cardCompany,"Credit");
        executeFile(cardCompany+"/debit.py");
        executeInsertData(cardCompany,"Debit");
    }
    private static void executeFile(String path) throws CustomException {
        try {
            Resource resource = ResourcePatternUtils
                    .getResourcePatternResolver(new DefaultResourceLoader())
                    .getResource("classpath*:crawling/"+path);
            InputStream inputStream = resource.getInputStream();
            LOG.info(String.valueOf(inputStream));

            File file =File.createTempFile("crawling/"+path,".py");
            FileUtils.copyInputStreamToFile(inputStream, file);

            ProcessBuilder pb = new ProcessBuilder("python", file.getPath());
            pb.redirectErrorStream(true);
            Process p = pb.start();
//            List<String> results = readProcessOutput(process.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

            List<String> results;

            results = br.lines().collect(Collectors.toList());

            for (String result : results) {
                LOG.info(result);
            }
            p.waitFor();

        } catch (Exception e){
            throw new CustomException(ResponseCode.CARD_BENEFITS_CRAWLING_FAIL);
        }
    }
    private static void executeInsertData(String firstInput, String secondInput) throws CustomException {
        try {
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

        } catch (Exception e){
            throw new CustomException(ResponseCode.CARD_BENEFITS_INSERT_FAIL);
        }
    }
}
