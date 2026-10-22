package aula.md2;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.FloatBuffer;

import org.junit.jupiter.api.Test;

// Testa só os métodos estáticos (não precisam de contexto OpenGL).
class Md2ModelTest {
	@Test
	void interpolaUmQuartoDoCaminho() {
		float[] s = new float[3];
		Md2Model.interpolar(new float[]{0, 10, -4}, new float[]{10, 20, 4}, 0.25f, s);
		assertArrayEquals(new float[]{2.5f, 12.5f, -2f}, s, 1e-6f);
	}

	@Test
	void tZeroDaAETUmDaB() {
		float[] a = {1, 2, 3};
		float[] b = {7, -8, 9};
		float[] s = new float[3];
		Md2Model.interpolar(a, b, 0f, s);
		assertArrayEquals(a, s, 1e-6f);
		Md2Model.interpolar(a, b, 1f, s);
		assertArrayEquals(b, s, 1e-6f);
	}

	@Test
	void expandePelosTriangulos() {
		FloatBuffer out = FloatBuffer.allocate(9);
		Md2Model.expandir(new float[]{0, 0, 0, 1, 1, 1, 2, 2, 2}, new int[]{2, 0, 1}, out);
		assertEquals(0, out.position());
		assertEquals(9, out.limit());
		float[] r = new float[9];
		out.get(r);
		assertArrayEquals(new float[]{2, 2, 2, 0, 0, 0, 1, 1, 1}, r);
	}
}
