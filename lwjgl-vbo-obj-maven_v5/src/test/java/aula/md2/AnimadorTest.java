package aula.md2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class AnimadorTest {
	private Animador novoAnimador() {
		ArrayList<Md2Animacao> lista = new ArrayList<>();
		lista.add(new Md2Animacao("stand", 0, 39));
		lista.add(new Md2Animacao("run", 40, 45));
		lista.add(new Md2Animacao("boom", 198, 198));
		return new Animador(lista);
	}

	@Test
	void comecaNoPrimeiroQuadroDaPrimeiraAnimacao() {
		Animador a = novoAnimador();
		assertEquals(0, a.quadroAtual());
		assertEquals(1, a.proximoQuadro());
		assertEquals(0f, a.fracao(), 1e-4);
		assertEquals(10, a.velocidade);
		assertTrue(a.interpolar);
		assertFalse(a.pausado);
	}

	@Test
	void avancaEInterpola() {
		Animador a = novoAnimador();
		a.proximaAnimacao();
		a.atualizar(0.135);
		assertEquals(41, a.quadroAtual());
		assertEquals(42, a.proximoQuadro());
		assertEquals(0.35f, a.fracao(), 1e-4);
	}

	@Test
	void ultimoQuadroVoltaParaOPrimeiro() {
		Animador a = novoAnimador();
		a.proximaAnimacao();
		a.atualizar(0.55);
		assertEquals(45, a.quadroAtual());
		assertEquals(40, a.proximoQuadro());
	}

	@Test
	void dtEnormeContinuaDentroDaAnimacao() {
		Animador a = novoAnimador();
		a.proximaAnimacao();
		a.atualizar(1000.37);
		assertTrue(a.quadroAtual() >= 40 && a.quadroAtual() <= 45);
		assertTrue(a.proximoQuadro() >= 40 && a.proximoQuadro() <= 45);
		assertTrue(a.fracao() >= 0 && a.fracao() < 1);
	}

	@Test
	void animacaoDeUmQuadroFicaParada() {
		Animador a = novoAnimador();
		a.animacaoAnterior();
		a.atualizar(0.37);
		assertEquals(198, a.quadroAtual());
		assertEquals(198, a.proximoQuadro());
	}

	@Test
	void setasDaoAVolta() {
		Animador a = novoAnimador();
		a.animacaoAnterior();
		assertEquals(2, a.indiceAnimacao());
		a.proximaAnimacao();
		assertEquals(0, a.indiceAnimacao());
	}

	@Test
	void trocarAnimacaoRecomecaDoPrimeiroQuadro() {
		Animador a = novoAnimador();
		a.atualizar(0.5);
		a.proximaAnimacao();
		assertEquals(40, a.quadroAtual());
		assertEquals(0f, a.fracao(), 1e-4);
	}

	@Test
	void interpolacaoDesligadaDaFracaoZero() {
		Animador a = novoAnimador();
		a.proximaAnimacao();
		a.atualizar(0.135);
		a.alternarInterpolacao();
		assertEquals(0f, a.fracao(), 1e-4);
		assertEquals(41, a.quadroAtual());
		assertFalse(a.interpolar);
	}

	@Test
	void pausadoNaoAvanca() {
		Animador a = novoAnimador();
		a.alternarPausa();
		a.atualizar(1.0);
		assertEquals(0, a.quadroAtual());
		assertTrue(a.pausado);
	}

	@Test
	void velocidadeVaiDe1a60() {
		Animador a = novoAnimador();
		a.aumentarVelocidade();
		assertEquals(15, a.velocidade);
		a.diminuirVelocidade();
		assertEquals(10, a.velocidade);
		a.diminuirVelocidade();
		assertEquals(9, a.velocidade);
		for (int i = 0; i < 20; i++) a.diminuirVelocidade();
		assertEquals(1, a.velocidade);
		for (int i = 0; i < 30; i++) a.aumentarVelocidade();
		assertEquals(60, a.velocidade);
	}

	@Test
	void descricaoUsaPontoDecimalMesmoEmPortugues() {
		Locale anterior = Locale.getDefault();
		try {
			Locale.setDefault(Locale.forLanguageTag("pt-BR"));
			Animador a = novoAnimador();
			a.proximaAnimacao();
			a.atualizar(0.135);
			assertEquals("run (2/3) | quadro 41 -> 42 | t=0.35 | interp ON | 10 fps", a.descricao());
			a.alternarInterpolacao();
			a.alternarPausa();
			assertTrue(a.descricao().contains("interp OFF"));
			assertTrue(a.descricao().endsWith(" | PAUSADO"));
		} finally {
			Locale.setDefault(anterior);
		}
	}
}
