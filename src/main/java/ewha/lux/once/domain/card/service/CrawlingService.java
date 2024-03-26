package ewha.lux.once.domain.card.service;

import ewha.lux.once.global.common.CustomException;
import ewha.lux.once.global.common.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CrawlingService {
    private static final Logger LOG = LoggerFactory.getLogger(CrawlingService.class);

    // 매주 월요일 00:00 카드 혜택 크롤링
    @Scheduled(cron = "0 45 16 ? * *")
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
            ProcessBuilder pb = new ProcessBuilder("python3", "/crawling/"+path);
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
            throw new CustomException(ResponseCode.CARD_BENEFITS_CRAWLING_FAIL);
        }
    }
    private static void executeInsertData(String firstInput, String secondInput) throws CustomException {
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "/crawling/DatabaseInsert.py",firstInput,secondInput);
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
