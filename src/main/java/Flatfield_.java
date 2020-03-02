/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */
// sPECIAL VERSION BY lAS kARLSSON
// Added z4, z5 and opticl center

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import java.lang.System;

public class Flatfield_ implements PlugIn {
	protected ImagePlus image;

	// image property members
	private int width;
	private int height;
	private int opt_cent_x; // In pixels
	private int opt_cent_y; // In pixels
	
	// plugin parameters
	private float a; // polynomial, constants
	private float b;
	private float c;
	private float d;
	private float e; // 4th degree

	@Override
	public void run(String value) {
		// get width, height and optical center

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
		GenericDialog gd = new GenericDialog("Generate flatfield from Excel sheet constants");

		// default value is 0.00, 2 digits right of the decimal point
<<<<<<< HEAD
		gd.addMessage("Flat image dimensions");
		gd.addNumericField("Width:", 4500, 0);
		gd.addNumericField("Height:", 3000, 0);
		gd.addNumericField("Optical center X:", 2250, 0);
		gd.addNumericField("Optical center Y:", 1500, 0);
		gd.addMessage("Polynomial constants");
		gd.addNumericField("a const:", 0.6, 10); // should be normalized to 1 in center !
		gd.addNumericField("b const:", 0.0000544, 10);
		gd.addNumericField("c const:", 0.000000247, 10);
		gd.addNumericField("d const:", -0.000000000115, 10);
		gd.addNumericField("e const:", 0.000000000000021, 10);
		gd.addMessage("Flat image should be normalized to =1 in center");
		gd.addMessage("Use Excel sheet to calculate constants");
		gd.addMessage("www.astrofriend.eu/astronomy/tutorials");
		
=======
		gd.addNumericField("Width:", 1024, 0);
		gd.addNumericField("Height:", 768, 0);
		gd.addNumericField("z⁰ factor:", 0.00, 2);
		gd.addNumericField("z¹ factor:", 20.00, 2);
		gd.addNumericField("z² factor:", 0.05, 2);

>>>>>>> branch 'master' of git@github.com:lboclboc/Flatfield.git
		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		// get entered values
		width = (int)gd.getNextNumber();
		height = (int)gd.getNextNumber();
		opt_cent_x = (int)gd.getNextNumber();
		opt_cent_y = (int)gd.getNextNumber();
		a = (float)gd.getNextNumber();
		b = (float)gd.getNextNumber();
		c = (float)gd.getNextNumber();
		d = (float)gd.getNextNumber();
		e = (float)gd.getNextNumber();
		
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
				float radie = Math.sqrt(Math.pow(x - opt_cent_x, 2) + Math.pow(y - opt_cent_y, 2)); // only integers
				float level = (a + b*radie + c*radie*radie + d*radie*radie*radie + e*radie*radie*radie*radie);
				if (level < 0.3) { // no corection of abnorm vignetting
					level = 0.3;
				}
				pixels[x + y * width] = level;
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
		System.out.println("plugins.dir: " + System.getProperty("plugins.dir"));

		// start ImageJ
		new ImageJ();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
