package aula.md2;

import java.util.ArrayList;
import java.util.Locale;

// Controla qual animação está tocando e em que ponto dela estamos.
// Não usa OpenGL: só faz contas com o tempo.
public class Animador {
	public int velocidade = 10;         // keyframes por segundo
	public boolean interpolar = true;
	public boolean pausado = false;

	private ArrayList<Md2Animacao> animacoes;
	private int indice = 0;             // animação atual (base 0)

	// Posição dentro da animação atual, medida em keyframes, sempre em [0, n).
	// Ex.: 3.4 = entre o quadro 3 e o quadro 4, a 40% do caminho.
	private double tempo = 0;

	public Animador(ArrayList<Md2Animacao> animacoes) {
		this.animacoes = animacoes;
	}

	// dt = segundos desde o quadro anterior.
	// dt * velocidade = quantos keyframes passaram. O % n faz o tempo dar a volta
	// no fim da animação (e também resolve um dt enorme, de uma só vez).
	public void atualizar(double dt) {
		if (pausado) return;
		int n = animacaoAtual().numQuadros();
		tempo = (tempo + dt * velocidade) % n;
	}

	public void proximaAnimacao() {
		indice = (indice + 1) % animacoes.size();
		tempo = 0;
	}

	public void animacaoAnterior() {
		indice = (indice - 1 + animacoes.size()) % animacoes.size();
		tempo = 0;
	}

	public void aumentarVelocidade() {
		velocidade = Math.min(60, velocidade < 10 ? velocidade + 1 : velocidade + 5);
	}

	public void diminuirVelocidade() {
		velocidade = Math.max(1, velocidade > 10 ? velocidade - 5 : velocidade - 1);
	}

	public void alternarInterpolacao() {
		interpolar = !interpolar;
	}

	public void alternarPausa() {
		pausado = !pausado;
	}

	public Md2Animacao animacaoAtual() {
		return animacoes.get(indice);
	}

	public int indiceAnimacao() {
		return indice;
	}

	// Parte inteira do tempo = quadro de onde partimos.
	public int quadroAtual() {
		return animacaoAtual().inicio + (int) Math.floor(tempo);
	}

	// Quadro seguinte; depois do último volta para o primeiro da animação.
	public int proximoQuadro() {
		int n = animacaoAtual().numQuadros();
		return animacaoAtual().inicio + ((int) Math.floor(tempo) + 1) % n;
	}

	// Parte fracionária do tempo = quanto já andamos até o próximo quadro (0 a 1).
	public float fracao() {
		return interpolar ? (float) (tempo - Math.floor(tempo)) : 0;
	}

	// Texto para o título da janela. Locale.US garante ponto decimal (0.35, não 0,35).
	public String descricao() {
		String texto = String.format(Locale.US, "%s (%d/%d) | quadro %d -> %d | t=%.2f | interp %s | %d fps",
				animacaoAtual().nome, indice + 1, animacoes.size(), quadroAtual(), proximoQuadro(),
				fracao(), interpolar ? "ON" : "OFF", velocidade);
		if (pausado) texto += " | PAUSADO";
		return texto;
	}
}
