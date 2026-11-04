package family.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SampleSizeTest {
    @Test public void fourTimesLargerSamplesByFour() { assertEquals(4, SampleSize.forTarget(1280, 720, 320, 180)); }
    @Test public void exactSizeIsOne() { assertEquals(1, SampleSize.forTarget(320, 180, 320, 180)); }
    @Test public void doubleSizeSamplesByTwo() { assertEquals(2, SampleSize.forTarget(640, 360, 320, 180)); }
    @Test public void zeroSizeIsOne() { assertEquals(1, SampleSize.forTarget(0, 0, 320, 180)); }
    @Test public void negativeTargetIsOne() { assertEquals(1, SampleSize.forTarget(640, 360, -1, 180)); }
    @Test public void neverSamplesBelowTheTarget() { assertEquals(1, SampleSize.forTarget(500, 281, 320, 180)); }
}
