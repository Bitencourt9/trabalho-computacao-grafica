package aula;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.opengl.GL;

import aula.md2.Animador;
import aula.md2.Md2Animacao;
import aula.md2.Md2Model;
import aula.md2.Skins;
import aula.util.TextureLoader;

import java.io.File;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Main {

	// Estado que o teclado altera (campos estáticos para o callback enxergar)
	static Animador animador;
	static int[] texturasCorpo = new int[0];
	static int skinIndice = 0;
	static boolean girando = true;

	// Lê a imagem e cria a textura. Se o arquivo não for uma imagem válida,
	// avisa no console e devolve 0 (sem textura), sem derrubar o programa.
	static int carregarTextura(File arquivo) {
		try {
			BufferedImage imagem = ImageIO.read(arquivo);   // null se o formato não for reconhecido
			if (imagem == null) {
				System.out.println("Aviso: imagem inválida, ignorada: " + arquivo);
				return 0;
			}
			return TextureLoader.loadTexture(imagem);
		} catch (IOException e) {
			System.out.println("Aviso: não foi possível ler a imagem " + arquivo + ": " + e.getMessage());
			return 0;
		}
	}

	public static void main(String[] args) throws Exception {

		String caminho = args.length > 0 ? args[0] : "src/main/resources/md2/ogro/ogro.md2";
		File pasta = new File(caminho).getAbsoluteFile().getParentFile();   // pasta do modelo

		if (!glfwInit())
			throw new IllegalStateException("GLFW nao iniciou");

		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
		long window = glfwCreateWindow(1024, 768, "MD2", 0, 0);
		if (window == 0)
			throw new IllegalStateException("Falha ao criar janela");
		glfwMakeContextCurrent(window);
		glfwSwapInterval(1);
		GL.createCapabilities();
		glEnable(GL_DEPTH_TEST);

		glClearColor(.5f, .5f, .5f, 1);
		try {
			// Modelo do corpo (e a arma, se existir weapon.md2 na mesma pasta)
			Md2Model corpo = new Md2Model(caminho);
			Md2Model arma = null;
			File arquivoArma = new File(pasta, "weapon.md2");
			if (arquivoArma.isFile()) {
				arma = new Md2Model(arquivoArma.getPath());
				// A arma usa os mesmos índices de quadro do corpo: precisa ter o mesmo número de quadros
				if (arma.arquivo.numQuadros != corpo.arquivo.numQuadros) {
					System.out.println("Aviso: weapon.md2 tem " + arma.arquivo.numQuadros + " quadros e o corpo tem "
							+ corpo.arquivo.numQuadros + "; desenhando só o corpo");
					arma = null;
				}
			} else
				System.out.println("Sem weapon.md2: desenhando só o corpo");

			// Carrega todas as skins de uma vez, começando pela que o cabeçalho pede
			File pastaSkins = new File(pasta, "skins");
			ArrayList<File> skins = Skins.listarSkinsCorpo(pastaSkins);
			texturasCorpo = new int[skins.size()];
			for (int i = 0; i < skins.size(); i++)
				texturasCorpo[i] = carregarTextura(skins.get(i));
			if (skins.isEmpty()) {
				System.out.println("Aviso: nenhuma skin em " + pastaSkins + " (sem textura)");
				corpo.textura = 0;
			} else {
				skinIndice = Skins.indiceInicial(skins, corpo.arquivo.skins);
				corpo.textura = texturasCorpo[skinIndice];
			}
			File skinArma = Skins.acharSkinArma(pastaSkins);
			if (arma != null && skinArma != null)
				arma.textura = carregarTextura(skinArma);

			animador = new Animador(Md2Animacao.montarLista(corpo.arquivo.nomesQuadros));

			// Teclas de alternância: agem uma vez por toque (GLFW_PRESS)
			glfwSetKeyCallback(window, (janela, tecla, scancode, acao, mods) -> {
				if (acao != GLFW_PRESS)
					return;
				if (tecla == GLFW_KEY_RIGHT) animador.proximaAnimacao();
				if (tecla == GLFW_KEY_LEFT) animador.animacaoAnterior();
				if (tecla == GLFW_KEY_I) animador.alternarInterpolacao();
				if (tecla == GLFW_KEY_EQUAL || tecla == GLFW_KEY_KP_ADD) animador.aumentarVelocidade();
				if (tecla == GLFW_KEY_MINUS || tecla == GLFW_KEY_KP_SUBTRACT) animador.diminuirVelocidade();
				if (tecla == GLFW_KEY_SPACE) animador.alternarPausa();
				if (tecla == GLFW_KEY_R) girando = !girando;
				if (tecla == GLFW_KEY_T && !skins.isEmpty()) {
					skinIndice = (skinIndice + 1) % skins.size();   // dá a volta
					corpo.textura = texturasCorpo[skinIndice];
				}
				if (tecla == GLFW_KEY_ESCAPE) glfwSetWindowShouldClose(janela, true);
			});

			System.out.println("Modelo: " + caminho + " | " + corpo.arquivo.numTriangulos + " triangulos");

			double tempoAnterior = glfwGetTime();
			double tempoTitulo = 0;
			float angulo = 0;
			while (!glfwWindowShouldClose(window)) {

				// Controle do tempo: dt = segundos desde o quadro anterior
				double agora = glfwGetTime();
				double dt = agora - tempoAnterior;
				tempoAnterior = agora;

				int[] w = new int[1], h = new int[1];
				glfwGetFramebufferSize(window, w, h);
				if (h[0] == 0) {
					glfwPollEvents();
					continue;
				}

				animador.atualizar(dt);
				if (girando)
					angulo += dt * 25;

				corpo.atualizarPose(animador.quadroAtual(), animador.proximoQuadro(), animador.fracao());
				if (arma != null)
					arma.atualizarPose(animador.quadroAtual(), animador.proximoQuadro(), animador.fracao());

				// Atualiza o título 10 vezes por segundo
				if (agora - tempoTitulo >= 0.1) {
					tempoTitulo = agora;
					String nomeSkin = skins.isEmpty() ? "sem textura" : Skins.nomeSemExtensao(skins.get(skinIndice));
					glfwSetWindowTitle(window, "MD2 | " + animador.descricao() + " | skin " + nomeSkin);
				}

				glViewport(0, 0, w[0], h[0]);
				glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
				glMatrixMode(GL_PROJECTION);
				glLoadIdentity();
				double aspect = (double) w[0] / h[0];
				glFrustum(-aspect * .7, aspect * .7, -.7, .7, 1, 100);
				glMatrixMode(GL_MODELVIEW);

				glLoadIdentity();
				glTranslatef(0, 0, -5);
				glRotatef(20, 1, 0, 0);          // inclinação fixa da câmera
				glRotatef(angulo, 0, 1, 0);      // rotação automática (tecla R)
				glRotatef(-90, 0, 1, 0);         // o MD2 olha para +X; -90° em Y leva +X para +Z (na direção da câmera)
				glScalef(0.07f, 0.07f, 0.07f);
				glRotatef(-90, 1, 0, 0);         // Quake tem Z para cima; OpenGL aqui usa Y

				// W segurado = wireframe (como no código-base)
				glPolygonMode(GL_FRONT_AND_BACK, glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS ? GL_LINE : GL_FILL);

				glEnable(GL_TEXTURE_2D);
				glColor3f(1, 1, 1);
				corpo.draw();
				if (arma != null)
					arma.draw();

				glfwSwapBuffers(window);
				glfwPollEvents();
			}
		} catch (IOException e) {
			System.err.println("Erro ao abrir MD2: " + e.getMessage());
		} finally {
			Callbacks.glfwFreeCallbacks(window);
			glfwDestroyWindow(window);
			glfwTerminate();
		}
	}
}
