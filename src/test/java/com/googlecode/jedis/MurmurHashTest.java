package com.googlecode.jedis;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.testng.annotations.Test;

/**
 * Test class for the Java Murmur hash implementation.
 * 
 * Public domain.
 * 
 * @author Viliam Holub
 * @version 1.0.2
 * 
 */
public class MurmurHashTest {

    /** Random input data with various length. */
    static final byte[][] input = {
	    { (byte) 0xed, (byte) 0x53, (byte) 0xc4, (byte) 0xa5, (byte) 0x3b,
		    (byte) 0x1b, (byte) 0xbd, (byte) 0xc2, (byte) 0x52,
		    (byte) 0x7d, (byte) 0xc3, (byte) 0xef, (byte) 0x53,
		    (byte) 0x5f, (byte) 0xae, (byte) 0x3b },
	    { (byte) 0x21, (byte) 0x65, (byte) 0x59, (byte) 0x4e, (byte) 0xd8,
		    (byte) 0x12, (byte) 0xf9, (byte) 0x05, (byte) 0x80,
		    (byte) 0xe9, (byte) 0x1e, (byte) 0xed, (byte) 0xe4,
		    (byte) 0x56, (byte) 0xbb },
	    { (byte) 0x2b, (byte) 0x02, (byte) 0xb1, (byte) 0xd0, (byte) 0x3d,
		    (byte) 0xce, (byte) 0x31, (byte) 0x3d, (byte) 0x97,
		    (byte) 0xc4, (byte) 0x91, (byte) 0x0d, (byte) 0xf7,
		    (byte) 0x17 },
	    { (byte) 0x8e, (byte) 0xa7, (byte) 0x9a, (byte) 0x02, (byte) 0xe8,
		    (byte) 0xb9, (byte) 0x6a, (byte) 0xda, (byte) 0x92,
		    (byte) 0xad, (byte) 0xe9, (byte) 0x2d, (byte) 0x21 },
	    { (byte) 0xa9, (byte) 0x6d, (byte) 0xea, (byte) 0x77, (byte) 0x06,
		    (byte) 0xce, (byte) 0x1b, (byte) 0x85, (byte) 0x48,
		    (byte) 0x27, (byte) 0x4c, (byte) 0xfe },
	    { (byte) 0xec, (byte) 0x93, (byte) 0xa0, (byte) 0x12, (byte) 0x60,
		    (byte) 0xee, (byte) 0xc8, (byte) 0x0a, (byte) 0xc5,
		    (byte) 0x90, (byte) 0x62 },
	    { (byte) 0x55, (byte) 0x6d, (byte) 0x93, (byte) 0x66, (byte) 0x14,
		    (byte) 0x6d, (byte) 0xdf, (byte) 0x00, (byte) 0x58,
		    (byte) 0x99 },
	    { (byte) 0x3c, (byte) 0x72, (byte) 0x20, (byte) 0x1f, (byte) 0xd2,
		    (byte) 0x59, (byte) 0x19, (byte) 0xdb, (byte) 0xa1 },
	    { (byte) 0x23, (byte) 0xa8, (byte) 0xb1, (byte) 0x87, (byte) 0x55,
		    (byte) 0xf7, (byte) 0x8a, (byte) 0x4b,

	    },
	    { (byte) 0xe2, (byte) 0x42, (byte) 0x1c, (byte) 0x2d, (byte) 0xc1,
		    (byte) 0xe4, (byte) 0x3e },
	    { (byte) 0x66, (byte) 0xa6, (byte) 0xb5, (byte) 0x5a, (byte) 0x74,
		    (byte) 0xd9 },
	    { (byte) 0xe8, (byte) 0x76, (byte) 0xa8, (byte) 0x90, (byte) 0x76 },
	    { (byte) 0xeb, (byte) 0x25, (byte) 0x3f, (byte) 0x87 },
	    { (byte) 0x37, (byte) 0xa0, (byte) 0xa9 },
	    { (byte) 0x5b, (byte) 0x5d }, { (byte) 0x7e }, {} };

    /*
     * Expected results - from the original C implementation.
     */

    /** Murmur 32bit hash results, special test seed. */
    static final int[] results32_seed = { 0xd92e493e, 0x8b50903b, 0xc3372a7b,
	    0x48f07e9e, 0x8a5e4a6e, 0x57916df4, 0xa346171f, 0x1e319c86,
	    0x9e1a03cd, 0x9f973e6c, 0x2d8c77f5, 0xabed8751, 0x296708b6,
	    0x24f8078b, 0x111b1553, 0xa7da1996, 0xfe776c70 };
    /** Murmur 32bit hash results, default library seed. */
    static final int[] results32_standard = { 0x96814fb3, 0x485dcaba,
	    0x331dc4ae, 0xc6a7bf2f, 0xcdf35de0, 0xd9dec7cc, 0x63a7318a,
	    0xd0d3c2de, 0x90923aef, 0xaf35c1e2, 0x735377b2, 0x366c98f3,
	    0x9c48ee29, 0x0b615790, 0xb4308ac1, 0xec98125a, 0x106e08d9 };
    /** Murmur 64bit hash results, special test seed. */
    static final long[] results64_seed = { 0x0822b1481a92e97bL,
	    0xf8a9223fef0822ddL, 0x4b49e56affae3a89L, 0xc970296e32e1d1c1L,
	    0xe2f9f88789f1b08fL, 0x2b0459d9b4c10c61L, 0x377e97ea9197ee89L,
	    0xd2ccad460751e0e7L, 0xff162ca8d6da8c47L, 0xf12e051405769857L,
	    0xdabba41293d5b035L, 0xacf326b0bb690d0eL, 0x0617f431bc1a8e04L,
	    0x15b81f28d576e1b2L, 0x28c1fe59e4f8e5baL, 0x694dd315c9354ca9L,
	    0xa97052a8f088ae6cL };
    /** Murmur 64bit hash results, default library seed. */
    static final long[] results64_standard = { 0x4987cb15118a83d9L,
	    0x28e2a79e3f0394d9L, 0x8f4600d786fc5c05L, 0xa09b27fea4b54af3L,
	    0x25f34447525bfd1eL, 0x32fad4c21379c7bfL, 0x4b30b99a9d931921L,
	    0x4e5dab004f936cdbL, 0x06825c27bc96cf40L, 0xff4bf2f8a4823905L,
	    0x7f7e950c064e6367L, 0x821ade90caaa5889L, 0x6d28c915d791686aL,
	    0x9c32649372163ba2L, 0xd66ae956c14d5212L, 0x38ed30ee5161200fL,
	    0x9bfae0a4e613fc3cL, };

    /** Dummy test text. */
    static final String text = "Lorem ipsum dolor sit amet, consectetur adipisicing elit";

    @Test
    public final void testHash32ByteArrayInt() {

	for (int i = 0; i < input.length; i++) {
	    final int hash = new MurmurHashingStrategy().hash32(input[i],
		    input[i].length);
	    if (hash != results32_standard[i]) {
		fail(String
			.format("Unexpected hash32 result for example %d: 0x%08x instead of 0x%08x",
				i, hash, results32_standard[i]));
	    }
	}
    }

    @Test
    public final void testHash32ByteArrayIntInt() {
	for (int i = 0; i < input.length; i++) {
	    final int hash = new MurmurHashingStrategy().hash32(input[i],
		    input[i].length, 0x71b4954d);
	    if (hash != results32_seed[i]) {
		fail(String
			.format("Unexpected hash32 result for example %d: 0x%08x instead of 0x%08x",
				i, hash, results32_seed[i]));
	    }
	}
    }

    @Test
    public final void testHash32String() {
	final int hash = new MurmurHashingStrategy().hash32(text);
	try {
	    Thread.sleep(20);
	} catch (final InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	assertTrue(hash == 0xb3bf597e);
    }

    @Test
    public final void testHash32StringIntInt() {
	final int hash = new MurmurHashingStrategy().hash32(text, 2,
		text.length() - 4);
	assertTrue(hash == 0x4d666d90);
    }

    @Test
    public final void testHash64ByteArrayInt() {
	for (int i = 0; i < input.length; i++) {
	    final long hash = new MurmurHashingStrategy().hash64(input[i],
		    input[i].length);
	    assertThat(
		    format("Unexpected hash64 result for example %d: 0x%016x instead of 0x%016x",
			    i, hash, results64_standard[i]), hash,
		    is(results64_standard[i]));
	}
    }

    @Test
    public final void testHash64ByteArrayIntInt() {
	for (int i = 0; i < input.length; i++) {
	    final long hash = new MurmurHashingStrategy().hash64(input[i],
		    input[i].length, 0x344d1f5c);
	    assertThat(
		    format("Unexpected hash64 result for example %d: 0x%016x instead of 0x%016x",
			    i, hash, results64_seed[i]), hash,
		    is(results64_seed[i]));
	}
    }

    @Test
    public final void testHash64String() {
	final long hash = new MurmurHashingStrategy().hash64(text);
	assertTrue(hash == 0x0920e0c1b7eeb261L);
    }

    @Test
    public final void testHash64StringIntInt() {
	final long hash = new MurmurHashingStrategy().hash64(text, 2,
		text.length() - 4);
	assertTrue(hash == 0xa8b33145194985a2L);
    }

}
