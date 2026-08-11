package com.aces.tennosquad.service;

import com.aces.tennosquad.exception.InvalidRequestException;
import com.aces.tennosquad.model.Relic;
import com.aces.tennosquad.model.enums.RelicEra;
import com.aces.tennosquad.repository.RelicRepository;
import com.microsoft.playwright.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RelicPullScriptService {
    private final RelicRepository relicRepository;

    public List<Relic> pullRelics() {
        List<Relic> scrapedRelics = getAllRelics("https://wiki.warframe.com/w/Prime_Resurgence");
        if (scrapedRelics.isEmpty()) {
            throw new InvalidRequestException(
                    "Relic refresh returned no relics"
            );
        }
        List<Relic> existingRelics = relicRepository.findAll();

        for(Relic relic : existingRelics){
            relic.setActive(false);
        }
        relicRepository.saveAll(existingRelics);

        List<Relic> updatedRelics = new ArrayList<>();

        for(Relic scrapedRelic : scrapedRelics){
            Relic relic = relicRepository
                    .findByNameIgnoreCase(scrapedRelic.getName())
                    .orElseGet(Relic::new);
            relic.setName(scrapedRelic.getName());
            relic.setRelicEra(scrapedRelic.getRelicEra());
            relic.setActive(true);

            updatedRelics.add(relicRepository.save(relic));
        }




        return updatedRelics;

    }





    private RelicEra setRelicEra(Relic relic){
        if(relic.getName().contains("Lith")){
            return RelicEra.LITH;
        }
        if(relic.getName().contains("Meso")){
            return RelicEra.MESO;
        }
        if(relic.getName().contains("Neo")){
            return RelicEra.NEO;
        }
        if(relic.getName().contains("Axi")){
            return RelicEra.AXI;
        }
        if(relic.getName().contains("Omnia")){
            return RelicEra.OMNIA;
        }
        else{
            return null;
        }
    }

    private List<Relic> getAllRelics(String url) {

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(false)
            );

            Page page = browser.newPage();
            page.navigate(url);

            List<Relic> allRelics = new ArrayList<>();

            Locator relicTable = page.locator(
                            "#mw-customcollapsible-resurgence " +
                                    "table.wikitable > tbody > tr"
                    ).first()
                    .locator("> td")
                    .nth(3)
                    .locator("table[data-tableid='CollectedRelics']");

            Locator relicElements = relicTable.locator("[data-param-name]");

            for (int i = 0; i < relicElements.count(); i++) {
                String name = relicElements.nth(i)
                        .getAttribute("data-param-name");

                Relic relic = new Relic();
                relic.setActive(true);
                relic.setName(name);
                relic.setRelicEra(setRelicEra(relic));
                allRelics.add(relic);
            }

            return allRelics;
        }
    }

}
