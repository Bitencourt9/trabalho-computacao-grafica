package aula.md2;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SkinsTest {
	static final String OGRO = "src/main/resources/md2/ogro/ogro.md2";
	static final String PASTA_SKINS = "src/main/resources/md2/ogro/skins";

	@Test
	void listaSkinsDoCorpoEmOrdemSemAArma(@TempDir Path d) throws IOException {
		Files.createFile(d.resolve("b.png"));
		Files.createFile(d.resolve("a.jpg"));
		Files.createFile(d.resolve("C.JPEG"));
		Files.createFile(d.resolve("weapon.jpg"));
		Files.createFile(d.resolve("notas.txt"));
		ArrayList<File> lista = Skins.listarSkinsCorpo(d.toFile());
		assertEquals(3, lista.size());
		assertEquals("a.jpg", lista.get(0).getName());
		assertEquals("b.png", lista.get(1).getName());
		assertEquals("C.JPEG", lista.get(2).getName());
	}

	@Test
	void pastaInexistenteDaListaVazia() {
		assertTrue(Skins.listarSkinsCorpo(new File("nao/existe")).isEmpty());
	}

	@Test
	void achaSkinDaArma(@TempDir Path d) throws IOException {
		assertNull(Skins.acharSkinArma(d.toFile()));
		Files.createFile(d.resolve("weapon.jpg"));
		assertEquals("weapon.jpg", Skins.acharSkinArma(d.toFile()).getName());
	}

	@Test
	void indiceInicialUsaPrimeiroSkinDoCabecalho() {
		ArrayList<File> skins = new ArrayList<File>();
		skins.add(new File("arboshak.png"));
		skins.add(new File("igdosh.png"));
		assertEquals(1, Skins.indiceInicial(skins, new String[]{"players/ogro/igdosh.pcx"}));
		assertEquals(0, Skins.indiceInicial(skins, new String[]{"x/naoexiste.pcx"}));
		assertEquals(0, Skins.indiceInicial(skins, new String[]{}));
	}

	@Test
	void skinsReaisDoOgroCarregam() throws IOException {
		File pasta = new File(PASTA_SKINS);
		ArrayList<File> lista = Skins.listarSkinsCorpo(pasta);
		assertEquals(13, lista.size());
		int inicial = Skins.indiceInicial(lista, new Md2Arquivo(OGRO).skins);
		assertEquals("igdosh.png", lista.get(inicial).getName());
		for (File f : lista) {
			assertNotNull(ImageIO.read(f), f.getName());
			assertEquals(256, ImageIO.read(f).getWidth(), f.getName());
		}
		java.awt.image.BufferedImage arma = ImageIO.read(Skins.acharSkinArma(pasta));
		assertEquals(256, arma.getWidth());
		assertEquals(128, arma.getHeight());
	}
}
