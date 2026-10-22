package aula.md2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

// Funções para descobrir os arquivos de skin (textura) de um modelo MD2.
public class Skins {

	// true se o nome termina em .png, .jpg ou .jpeg (maiúsculas ou minúsculas)
	static boolean ehImagem(String nome) {
		String minusculo = nome.toLowerCase();
		return minusculo.endsWith(".png") || minusculo.endsWith(".jpg") || minusculo.endsWith(".jpeg");
	}

	// true se o nome começa com "weapon" (skin da arma, não do corpo)
	static boolean ehDaArma(String nome) {
		return nome.toLowerCase().startsWith("weapon");
	}

	// Imagens da pasta, sem as da arma, em ordem alfabética (sem diferenciar maiúsculas).
	public static ArrayList<File> listarSkinsCorpo(File pasta) {
		ArrayList<File> lista = new ArrayList<File>();
		File[] arquivos = pasta.listFiles();   // null se a pasta não existe
		if (arquivos == null)
			return lista;
		for (File f : arquivos) {
			if (ehImagem(f.getName()) && !ehDaArma(f.getName()))
				lista.add(f);
		}
		Collections.sort(lista, new Comparator<File>() {
			public int compare(File a, File b) {
				return String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName());
			}
		});
		return lista;
	}

	// Primeiro weapon.png / weapon.jpg / weapon.jpeg da pasta, ou null.
	public static File acharSkinArma(File pasta) {
		String[] nomes = {"weapon.png", "weapon.jpg", "weapon.jpeg"};
		for (String nome : nomes) {
			File f = new File(pasta, nome);
			if (f.isFile())
				return f;
		}
		return null;
	}

	// Posição da skin que o cabeçalho do MD2 pede ("players/ogro/igdosh.pcx" -> igdosh).
	// Se não achar, ou se o cabeçalho estiver vazio, começa na primeira (0).
	public static int indiceInicial(ArrayList<File> skins, String[] skinsCabecalho) {
		if (skinsCabecalho.length == 0)
			return 0;
		String caminho = skinsCabecalho[0];
		String nomeArquivo = caminho.substring(caminho.lastIndexOf('/') + 1);
		int ponto = nomeArquivo.lastIndexOf('.');
		String nomeBase = ponto >= 0 ? nomeArquivo.substring(0, ponto) : nomeArquivo;
		for (int i = 0; i < skins.size(); i++) {
			if (nomeSemExtensao(skins.get(i)).equals(nomeBase))
				return i;
		}
		return 0;
	}

	public static String nomeSemExtensao(File f) {
		String nome = f.getName();
		int ponto = nome.lastIndexOf('.');
		return ponto >= 0 ? nome.substring(0, ponto) : nome;
	}
}
