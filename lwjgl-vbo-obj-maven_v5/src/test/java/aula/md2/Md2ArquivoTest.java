package aula.md2;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

class Md2ArquivoTest {
    static final String OGRO = "src/main/resources/md2/ogro/ogro.md2";
    static final String ARMA = "src/main/resources/md2/ogro/weapon.md2";

    @Test void leCabecalhoDoOgro() throws IOException {
        Md2Arquivo m = new Md2Arquivo(OGRO);
        assertEquals(256, m.larguraSkin); assertEquals(256, m.alturaSkin);
        assertEquals(358, m.numVertices); assertEquals(469, m.numSt);
        assertEquals(670, m.numTriangulos); assertEquals(199, m.numQuadros);
        assertEquals(13, m.skins.length);
        assertEquals("players/ogro/igdosh.pcx", m.skins[0]);
    }

    @Test void leCabecalhoDaArma() throws IOException {
        Md2Arquivo m = new Md2Arquivo(ARMA);
        assertEquals(256, m.larguraSkin); assertEquals(128, m.alturaSkin);
        assertEquals(74, m.numVertices); assertEquals(116, m.numSt);
        assertEquals(122, m.numTriangulos); assertEquals(199, m.numQuadros);
    }

    @Test void leUvsETriangulos() throws IOException {
        Md2Arquivo m = new Md2Arquivo(OGRO);
        assertEquals(469 * 2, m.uvs.length);
        assertEquals(0.99609375f, m.uvs[0], 1e-6); // s=255 / 256
        assertEquals(0.0390625f, m.uvs[1], 1e-6);  // t=10 / 256
        assertEquals(670 * 3, m.triVertices.length);
        assertArrayEquals(new int[]{330, 332, 331}, java.util.Arrays.copyOf(m.triVertices, 3));
        assertArrayEquals(new int[]{0, 1, 2}, java.util.Arrays.copyOf(m.triUvs, 3));
    }

    @Test void descompactaPosicoesDosQuadros() throws IOException {
        Md2Arquivo m = new Md2Arquivo(OGRO);
        assertEquals(199, m.posicoes.length);
        assertEquals(358 * 3, m.posicoes[0].length);
        assertEquals("stand001", m.nomesQuadros[0]);
        assertEquals("run001", m.nomesQuadros[40]);
        assertEquals("boom", m.nomesQuadros[198]);
        assertEquals(0.562324f, m.posicoes[0][0], 1e-4);
        assertEquals(1.466234f, m.posicoes[0][1], 1e-4);
        assertEquals(-2.718453f, m.posicoes[0][2], 1e-4);
        assertEquals(-1.851437f, m.posicoes[40][0], 1e-4);
        assertEquals(-0.233660f, m.posicoes[40][1], 1e-4);
        assertEquals(0.940164f, m.posicoes[40][2], 1e-4);
        assertEquals(8.105442f, m.posicoes[0][357 * 3], 1e-4);     // último vértice
        assertEquals(-20.559797f, m.posicoes[0][357 * 3 + 2], 1e-4);
    }

    @Test void rejeitaArquivoQueNaoEMd2() {
        byte[] lixo = "# isto e um OBJ\nv 0 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 3\n# mais texto para passar de 68 bytes\n".getBytes();
        IOException e = assertThrows(IOException.class, () -> new Md2Arquivo(lixo));
        assertTrue(e.getMessage().contains("MD2"), e.getMessage());
    }

    @Test void rejeitaArquivoCurtoDemais() {
        IOException e = assertThrows(IOException.class, () -> new Md2Arquivo(new byte[10]));
        assertTrue(e.getMessage().contains("MD2"), e.getMessage());
    }

    @Test void rejeitaVersaoErrada() {
        ByteBuffer b = ByteBuffer.allocate(68).order(ByteOrder.LITTLE_ENDIAN);
        b.put("IDP2".getBytes()).putInt(7);
        IOException e = assertThrows(IOException.class, () -> new Md2Arquivo(b.array()));
        assertTrue(e.getMessage().contains("MD2") && e.getMessage().contains("7"), e.getMessage());
    }

    @Test void arquivoInexistenteDaMensagemClara() {
        IOException e = assertThrows(IOException.class, () -> new Md2Arquivo("nao/existe.md2"));
        assertTrue(e.getMessage().contains("nao/existe.md2"), e.getMessage());
    }
}
