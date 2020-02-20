/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package se.cecilia_data.imagej;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Flatfield_ implements PlugIn {
	protected ImagePlus image;

	// image property members
	private int width;
	private int height;

	// plugin parameters
	private double z0;
	private double z1;
	private double z2;

	@Override
	public void run(String value) {
		// get width and height

		if (showDialog()) {
			// open the Clown sample
			image = IJ.createImage("Flatfield", width, height, 1, 32);
			if (image == null) {
				throw new RuntimeException("Null image");
			}
			process(image.getProcessor());
			image.show();
			image.updateAndDraw();
		}
	}

	private boolean showDialog() {
		GenericDialog gd = new GenericDialog("Generate flatfield");

		// default value is 0.00, 2 digits right of the decimal point
		gd.addNumericField("Width:", 1024, 0);
		gd.addNumericField("Height:", 768, 0);
		gd.addNumericField("z⁰ factor:", 0.00, 2);
		gd.addNumericField("z¹ factor:", 0.00, 2);
		gd.addNumericField("z² factor:", 0.00, 2);

		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		// get entered values
		width = (int)gd.getNextNumber();
		height = (int)gd.getNextNumber();
		z0 = gd.getNextNumber();
		z1 = gd.getNextNumber();
		z2 = gd.getNextNumber();

		return true;
	}

	/**
	 * Process an image.
	 * <p>
	 * Please provide this method even if {@link ij.plugin.filter.PlugInFilter} does require it;
	 * the method {@link ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)} can only
	 * handle 2-dimensional data.
	 * </p>
	 * <p>
	 * If your plugin does not change the pixels in-place, make this method return the results and
	 * change the {@link #setup(java.lang.String, ij.ImagePlus)} method to return also the
	 * <i>DOES_NOTHING</i> flag.
	 * </p>
	 *
	 * @param image the image (possible multi-dimensional)
	 */
	public void process(ImagePlus image) {
		// slice numbers start with 1 for historical reasons
		for (int i = 1; i <= image.getStackSize(); i++)
			process(image.getStack().getProcessor(i));
	}

	// Select processing method depending on image type
	public void process(ImageProcessor ip) {
		int type = image.getType();
		if (type == ImagePlus.GRAY32)
			process( (float[]) ip.getPixels() );
		else {
			throw new RuntimeException("not supported: " + Integer.toString(type));
		}
	}

	// processing of COLOR_RGB images
	public void process(float[] pixels) {
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				// process each pixel of the line
				// example: add 'number' to each pixel
				double z = Math.sqrt(Math.pow(x - width/2, 2) + Math.pow(y - height/2, 2));
				pixels[x + y * width] += z0 + z1*z + z2*z*z;
			}
		}
	}

	public void showAbout() {
		IJ.showMessage("Flatfield", "a flatfield simulator");
	}

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads
	 * an image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) throws Exception {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		// see: https://stackoverflow.com/a/7060464/1207769
		Class<?> clazz = Flatfield_.class;
		java.net.URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
		java.io.File file = new java.io.File(url.toURI());
		System.setProperty("plugins.dir", file.getAbsolutePath());

		// start ImageJ
		new ImageJ();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
