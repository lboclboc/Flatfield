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
	
	// plugin parameters polynomial
	private double a; // constant
	private double b; // not used yet, rotation symmetric
	private double c; // 2th degree
	private double d; // not used yet, rotation symmetric
	private double e; // 4th degree
	private double f; // not used yet, rotation symmetric
	private double g; // 6th degree

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
		GenericDialog gd = new GenericDialog("Flatfield");
		
		String[] exponentDef = {"-21","-20","-19","-18","-17","-16","-15","-14","-13","-12","-11","-10","-09","-08","-07","-06","-05","-04","-03","-02","-01","0","+01","+02"};
		// position                0     1     2     3     4     5     6     7     8     9    10    11    12    13    14    15    16    17    18    19    20  21    22    23
		
		// default value is 0.00, 2 digits right of the decimal point
		
		gd.addMessage("Generate flat image from polynomial");
		gd.addMessage("Flat image dimensions");
		gd.addNumericField("Width:", 2238, 0, 6, "pixel");
		gd.addNumericField("Height:", 1477, 0, 6, "pixel");
		gd.addNumericField("Optical center X:", 1100, 0, 6, "pixel");
		gd.addNumericField("Optical center Y:", 730, 0, 6, "pixel");
		
		gd.addMessage("Polynomial constants");
		gd.addNumericField("a bas: ", +9.57, 5, 7, "* 10^exp * r^0"); // should be normalized to 1 in center !gd.addToSameRow();
		gd.addChoice("a exp: ", exponentDef, exponentDef[20]);
						
		gd.addNumericField("c bas:", -9.4, 5, 7, "* 10^exp * r^2");
		gd.addChoice("c exp: ", exponentDef, exponentDef[14]);
						
		gd.addNumericField("e bas: ", +1.3, 5, 7, "* 10^exp * r^4");
		gd.addChoice("e exp: ", exponentDef, exponentDef[8]);
		
		gd.addNumericField("d bas: ", -6.7, 5, 7, "* 10^exp * r^6, only in difficult case");
		gd.addChoice("d exp: ", exponentDef, exponentDef[2]);
				
		gd.addMessage("Choose parameters to get it normalized to =1 in center");
		gd.addMessage("Values lower than 0.3, i.e. vignetting of 70% will be cut");
		gd.addMessage("Use Excel sheet to simulate curve and calculate constants");
		gd.addMessage("www.astrofriend.eu/astronomy/tutorials");
		gd.addMessage("See AstroImageJ tutorial page 3");
		gd.addMessage(" ");
		gd.addMessage("Version 20200304");
		
		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		// get entered values
		width = (int)gd.getNextNumber();
		height = (int)gd.getNextNumber();
		opt_cent_x = (int)gd.getNextNumber();
		opt_cent_y = (int)gd.getNextNumber();
		
		double a_bas = gd.getNextNumber();
		String a_exp = gd.getNextChoice();
		a = a_bas * Math.pow(10, Double.parseDouble(a_exp));
			
		b = 0; // spare part for future maybe
				
		double c_bas = gd.getNextNumber();
		String c_exp = gd.getNextChoice();
		c = c_bas * Math.pow(10, Double.parseDouble(c_exp));
		
		d = 0; // spare part for future maybe
				
		double e_bas = gd.getNextNumber();
		String e_exp = gd.getNextChoice();
		e = e_bas * Math.pow(10, Double.parseDouble(e_exp));
		
		d = 0; // spare part for future maybe
		
		double g_bas = gd.getNextNumber();
		String g_exp = gd.getNextChoice();
		g = g_bas * Math.pow(10, Double.parseDouble(g_exp));
		
		// Check calculations:
		System.out.println("A constant = " + a + ", B constant = " + b + ", C constant = " + c + ", D constant = " + d + ", E constant = " + e + ", F constant = " + f + ", G constant = " + g);
		
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
				double radie = Math.sqrt(Math.pow(x - opt_cent_x, 2) + Math.pow(y - opt_cent_y, 2)); // only integers
				float level = (float)(a + c*Math.pow(radie, 2) + e*Math.pow(radie, 4) + g*Math.pow(radie, 6));
				if (level < 0.3f) { // no correction of abnorm vignetting
					level = 0.3f;
				}
				pixels[x + y * width] = level; // built flat image with 32 bit float
			}
		}
		
		// Check calculations radius:
		double radius = Math.sqrt(Math.pow(0 - opt_cent_x, 2) + Math.pow(730 - opt_cent_y, 2));
		System.out.println("Radiou at x=0 = " + radius);
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
