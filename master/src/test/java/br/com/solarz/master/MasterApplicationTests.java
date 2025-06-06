package br.com.solarz.master;

import br.com.solarz.master.helpers.PopulateDatabaseHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MasterApplicationTests {

	@Autowired
	private PopulateDatabaseHelper populate;

	@Test
	void contextLoads() {
		populate.populateApis();
		populate.populateCredenciais();
		populate.populateUsinas();
	}

}
