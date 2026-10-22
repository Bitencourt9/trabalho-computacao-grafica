package aula.md2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

// Lê um arquivo MD2 (modelo animado do Quake 2) e guarda os dados em arrays.
// Não usa OpenGL: só lê bytes. Tudo no arquivo é little-endian.
public class Md2Arquivo {
	public int larguraSkin, alturaSkin;
	public int numVertices, numSt, numTriangulos, numQuadros;
	public String[] skins;          // nomes de textura gravados no cabeçalho
	public float[] uvs;             // numSt * 2, já divididos pelo tamanho da skin
	public int[] triVertices;       // numTriangulos * 3 (índices de vértice)
	public int[] triUvs;            // numTriangulos * 3 (índices de UV)
	public String[] nomesQuadros;   // numQuadros
	public float[][] posicoes;      // [numQuadros][numVertices * 3], já descompactadas

	public Md2Arquivo(String caminho) throws IOException {
		this(lerBytes(caminho));
	}

	private static byte[] lerBytes(String caminho) throws IOException {
		try {
			return Files.readAllBytes(Paths.get(caminho));
		} catch (NoSuchFileException e) {
			throw new IOException("Arquivo não encontrado: " + caminho);
		}
	}

	public Md2Arquivo(byte[] dados) throws IOException {
		if (dados.length < 68) {
			throw new IOException("Não é um arquivo MD2 (arquivo curto demais: " + dados.length + " bytes)");
		}
		ByteBuffer b = ByteBuffer.wrap(dados).order(ByteOrder.LITTLE_ENDIAN);

		// Cabeçalho: 17 inteiros de 4 bytes (68 bytes)
		String ident = new String(dados, 0, 4, StandardCharsets.US_ASCII);
		if (!ident.equals("IDP2")) {
			throw new IOException("Não é um arquivo MD2 (ident lido: " + ident + ")");
		}
		int versao = b.getInt(4);
		if (versao != 8) {
			throw new IOException("Versão de MD2 não suportada: " + versao);
		}
		larguraSkin = b.getInt(8);
		alturaSkin = b.getInt(12);
		int tamanhoQuadro = b.getInt(16);
		int numSkins = b.getInt(20);
		numVertices = b.getInt(24);
		numSt = b.getInt(28);
		numTriangulos = b.getInt(32);
		// posição 36 é num_glcmds (comandos GL), que não usamos
		numQuadros = b.getInt(40);
		int ofsSkins = b.getInt(44);
		int ofsSt = b.getInt(48);
		int ofsTris = b.getInt(52);
		int ofsQuadros = b.getInt(56);

		// Skins: cada nome ocupa 64 bytes
		skins = new String[numSkins];
		for (int i = 0; i < numSkins; i++) {
			skins[i] = lerTexto(b, ofsSkins + i * 64, 64);
		}

		// Coordenadas de textura: (short s, short t) em pixels; dividimos pelo tamanho da skin.
		// O v NÃO é invertido (a convenção do MD2 é a mesma do TextureLoader).
		uvs = new float[numSt * 2];
		for (int i = 0; i < numSt; i++) {
			uvs[i * 2] = b.getShort(ofsSt + i * 4) / (float) larguraSkin;
			uvs[i * 2 + 1] = b.getShort(ofsSt + i * 4 + 2) / (float) alturaSkin;
		}

		// Triângulos: 3 índices de vértice + 3 índices de UV (shorts), separados como no OBJ
		triVertices = new int[numTriangulos * 3];
		triUvs = new int[numTriangulos * 3];
		for (int i = 0; i < numTriangulos; i++) {
			int ofs = ofsTris + i * 12;
			for (int k = 0; k < 3; k++) {
				triVertices[i * 3 + k] = b.getShort(ofs + k * 2);
				triUvs[i * 3 + k] = b.getShort(ofs + 6 + k * 2);
			}
		}

		// Quadros: scale (3 floats), translate (3 floats), nome (16 bytes),
		// e depois numVertices * (x, y, z, índice da normal), cada um 1 byte
		nomesQuadros = new String[numQuadros];
		posicoes = new float[numQuadros][numVertices * 3];
		for (int q = 0; q < numQuadros; q++) {
			int ofs = ofsQuadros + q * tamanhoQuadro;
			float[] escala = { b.getFloat(ofs), b.getFloat(ofs + 4), b.getFloat(ofs + 8) };
			float[] translacao = { b.getFloat(ofs + 12), b.getFloat(ofs + 16), b.getFloat(ofs + 20) };
			nomesQuadros[q] = lerTexto(b, ofs + 24, 16);
			// Descompactação: posição real = escala * byte (sem sinal) + translação
			for (int v = 0; v < numVertices; v++) {
				int ofsVertice = ofs + 40 + v * 4;
				for (int eixo = 0; eixo < 3; eixo++) {
					int byteSemSinal = b.get(ofsVertice + eixo) & 0xFF;
					posicoes[q][v * 3 + eixo] = escala[eixo] * byteSemSinal + translacao[eixo];
				}
			}
		}
	}

	// Lê um texto de tamanho fixo, que termina no primeiro byte zero
	private static String lerTexto(ByteBuffer b, int ofs, int tamanho) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tamanho; i++) {
			byte c = b.get(ofs + i);
			if (c == 0) break;
			sb.append((char) c);
		}
		return sb.toString();
	}
}
