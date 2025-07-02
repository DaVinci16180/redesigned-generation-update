package br.com.solarz.master.helpers;

import br.com.solarz.master.model.Api;
import br.com.solarz.master.model.Credencial;
import br.com.solarz.master.model.Usina;
import br.com.solarz.master.model.Usina.Priority;
import br.com.solarz.master.repository.ApiRepository;
import br.com.solarz.master.repository.CredencialRepository;
import br.com.solarz.master.repository.UsinaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class PopulateDatabaseHelper {

    private final CredencialRepository credencialRepository;
    private final UsinaRepository usinaRepository;
    private final ApiRepository apiRepository;

    public void populateApis() {
        int apisCount = 50;
        List<Api> apis = apiRepository.findAll();

        if (!apis.isEmpty())
            return;

        for (long i = 1; i <= apisCount; i++) {
            Api api = new Api(i, "Portal" + i);
            apiRepository.save(api);

            int percentage = (int) (((double) i / (double) apisCount) * 100.);
            System.out.println("Portal " + api.getName() + " criado com sucesso (" + percentage + "%)");
        }
    }

    public void populateCredenciais() {
        Random rand = new Random();
        int credenciaisPorApiMin = 5;
        int credenciaisPorApiMax = 30;

        List<Credencial> credenciais = credencialRepository.findAll();

        if (!credenciais.isEmpty())
            return;

        List<Api> apis = apiRepository.findAll();
        long sequence = 1;
        for (Api api : apis) {
            int credenciaisAmount = rand.nextInt(credenciaisPorApiMin, credenciaisPorApiMax);

            for (int i = 0; i < credenciaisAmount; i++) {
                Credencial credencial = new Credencial(sequence++, api);
                credencial = credencialRepository.save(credencial);

                System.out.println("Credencial " + credencial.getId() + " do portal " + api.getName() + "criada com sucesso");
            }
        }
    }

    public void populateUsinas() {
        Random rand = new Random();
        int usinasPorCredencialMin = 1;
        int usinasPorCredencialMax = 40;

        List<Usina> usinas = usinaRepository.findAll();

        if (!usinas.isEmpty())
            return;

        List<Credencial> credenciais = credencialRepository.findAll();
        long sequence = 1;
        Random random = new Random(1);
        double priorityRate = 0.3;

        for (Credencial credencial : credenciais) {
            int usinasAmount = rand.nextInt(usinasPorCredencialMin, usinasPorCredencialMax);

            for (long i = 0; i < usinasAmount; i++) {
                boolean priority = random.nextDouble() < priorityRate;
                Usina usina = new Usina(sequence++, credencial, priority ? Priority.HIGH : Priority.NORMAL, 0, false);
                usina = usinaRepository.save(usina);

                System.out.println("Usina " + usina.getId() + " da credencial " + credencial.getId() + " criada com sucesso");
            }
        }
    }
}
