package aula.md2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class Md2AnimacaoTest {
	private static final String OGRO = "src/main/resources/md2/ogro/ogro.md2";

	private void confere(ArrayList<Md2Animacao> lista, int i, String nome, int inicio, int fim) {
		assertEquals(nome, lista.get(i).nome);
		assertEquals(inicio, lista.get(i).inicio);
		assertEquals(fim, lista.get(i).fim);
	}

	@Test
	void agrupaAs21AnimacoesDoOgro() throws IOException {
		ArrayList<Md2Animacao> lista = Md2Animacao.montarLista(new Md2Arquivo(OGRO).nomesQuadros);
		assertEquals(21, lista.size());
		confere(lista, 0, "stand", 0, 39);
		confere(lista, 1, "run", 40, 45);
		confere(lista, 3, "paina", 54, 57);
		confere(lista, 8, "salute_alt", 84, 94);
		confere(lista, 19, "deathc", 190, 197);
		confere(lista, 20, "boom", 198, 198);
	}

	@Test
	void agrupaSoQuadrosConsecutivos() {
		ArrayList<Md2Animacao> lista = Md2Animacao.montarLista(new String[] { "a1", "a2", "b1", "a3" });
		assertEquals(3, lista.size());
		confere(lista, 0, "a", 0, 1);
		confere(lista, 1, "b", 2, 2);
		confere(lista, 2, "a", 3, 3);
	}
}
