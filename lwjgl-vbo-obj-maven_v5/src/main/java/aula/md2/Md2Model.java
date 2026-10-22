package aula.md2;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

import java.io.IOException;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import aula.Model.Model;

// Modelo MD2 animado: interpola dois quadros-chave na CPU e desenha com VBO.
// Precisa de contexto OpenGL ativo para ser construído (usa glGenBuffers).
public class Md2Model extends Model {
	public Md2Arquivo arquivo;
	public int textura = 0;      // 0 = sem textura

	private int vboPosicoes;     // muda a cada quadro (GL_STREAM_DRAW)
	private int vboUvs;          // nunca muda (GL_STATIC_DRAW)
	private float[] poseAtual;           // numVertices * 3, resultado da interpolação
	private FloatBuffer bufferPosicoes;  // numTriangulos * 3 * 3, enviado à GPU

	public Md2Model(String caminho) throws IOException {
		arquivo = new Md2Arquivo(caminho);
		load();
	}

	// a + (b - a) * t : t = 0 dá o ponto A, t = 1 dá o ponto B
	public static void interpolar(float[] a, float[] b, float t, float[] saida) {
		for (int i = 0; i < saida.length; i++) {
			saida[i] = a[i] + (b[i] - a[i]) * t;
		}
	}

	// Copia, para cada índice de triangulo, os 3 floats (x, y, z) do vértice,
	// formando a lista de vértices na ordem em que o glDrawArrays vai ler.
	public static void expandir(float[] posVertices, int[] triVertices, FloatBuffer saida) {
		saida.clear();
		for (int i = 0; i < triVertices.length; i++) {
			int v = triVertices[i] * 3;
			saida.put(posVertices[v]);
			saida.put(posVertices[v + 1]);
			saida.put(posVertices[v + 2]);
		}
		saida.flip();
	}

	@Override
	public void load() {
		int total = arquivo.numTriangulos * 3;

		// UVs: para cada vértice de cada triângulo, pega o UV pelo índice triUvs
		FloatBuffer dadosUvs = BufferUtils.createFloatBuffer(total * 2);
		for (int i = 0; i < total; i++) {
			int uv = arquivo.triUvs[i] * 2;
			dadosUvs.put(arquivo.uvs[uv]);
			dadosUvs.put(arquivo.uvs[uv + 1]);
		}
		dadosUvs.flip();

		vboUvs = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboUvs);
		glBufferData(GL_ARRAY_BUFFER, dadosUvs, GL_STATIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER, 0);

		// Posições: o VBO é criado aqui e preenchido em atualizarPose
		vboPosicoes = glGenBuffers();
		poseAtual = new float[arquivo.numVertices * 3];
		bufferPosicoes = BufferUtils.createFloatBuffer(total * 3);

		atualizarPose(0, 0, 0);
	}

	// A pose é calculada na CPU e reenviada para a GPU todo quadro.
	public void atualizarPose(int quadroA, int quadroB, float t) {
		interpolar(arquivo.posicoes[quadroA], arquivo.posicoes[quadroB], t, poseAtual);
		expandir(poseAtual, arquivo.triVertices, bufferPosicoes);

		glBindBuffer(GL_ARRAY_BUFFER, vboPosicoes);
		glBufferData(GL_ARRAY_BUFFER, bufferPosicoes, GL_STREAM_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
	}

	@Override
	public void draw() {
		glBindTexture(GL_TEXTURE_2D, textura);

		glEnableClientState(GL_VERTEX_ARRAY);
		glBindBuffer(GL_ARRAY_BUFFER, vboPosicoes);
		glVertexPointer(3, GL_FLOAT, 0, 0L);
		glEnableClientState(GL_TEXTURE_COORD_ARRAY);
		glBindBuffer(GL_ARRAY_BUFFER, vboUvs);
		glTexCoordPointer(2, GL_FLOAT, 0, 0L);
		glDrawArrays(GL_TRIANGLES, 0, arquivo.numTriangulos * 3);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glDisableClientState(GL_TEXTURE_COORD_ARRAY);
		glDisableClientState(GL_VERTEX_ARRAY);
	}
}
