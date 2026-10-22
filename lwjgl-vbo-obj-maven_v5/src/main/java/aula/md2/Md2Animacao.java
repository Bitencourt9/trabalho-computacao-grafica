package aula.md2;

import java.util.ArrayList;

// Uma animação do MD2: um trecho de quadros consecutivos com o mesmo prefixo no nome
// (ex.: run1..run6 vira "run", do quadro 40 ao 45).
public class Md2Animacao {
	public String nome;
	public int inicio;   // índice do primeiro quadro
	public int fim;      // índice do último quadro (inclusive)

	public Md2Animacao(String nome, int inicio, int fim) {
		this.nome = nome;
		this.inicio = inicio;
		this.fim = fim;
	}

	public int numQuadros() {
		return fim - inicio + 1;
	}

	// Agrupa os quadros pelo prefixo do nome (nome sem os dígitos finais).
	// Só agrupa quadros vizinhos: "a1","a2","b1","a3" vira a, b, a.
	public static ArrayList<Md2Animacao> montarLista(String[] nomesQuadros) {
		ArrayList<Md2Animacao> lista = new ArrayList<>();
		for (int i = 0; i < nomesQuadros.length; i++) {
			String prefixo = nomesQuadros[i].replaceAll("\\d+$", "");
			Md2Animacao ultima = lista.isEmpty() ? null : lista.get(lista.size() - 1);
			if (ultima != null && ultima.nome.equals(prefixo)) {
				ultima.fim = i;   // mesmo prefixo da anterior: estende a animação
			} else {
				lista.add(new Md2Animacao(prefixo, i, i));
			}
		}
		return lista;
	}
}
