package br.com.solarz.pv_api_simulator.utils;

import br.com.solarz.pv_api_simulator.model.Portal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortalLoader {

    public static Map<Integer, Portal> carregarPortais(String caminhoJson) {
        ObjectMapper mapper = new ObjectMapper();
        Map<Integer, Portal> mapaPortais = new HashMap<>();

        try {
            List<Portal> listaPortais = mapper.readValue(new File(caminhoJson), new TypeReference<>() {});
            for (Portal portal : listaPortais)
                mapaPortais.put(portal.getId(), portal);
        } catch (IOException e) {
            System.err.println("Erro ao carregar o arquivo JSON: " + e.getMessage());
        }

        return mapaPortais;
    }
}
