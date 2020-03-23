/*
 * Syntetic Flatfield Generation
 * See github: https://github.com/lboclboc/Flatfield
 */
// Special version by Lars Karlsson

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import java.lang.System;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.awt.Choice;
import java.awt.TextField;
import java.awt.event.ItemEvent;


class Rectangle
{
	public Rectangle(int w, int h) {
		width = w;
		height = h;
	}
	public int width;
	public int height;
}


class FlatfieldPreset
{
	public FlatfieldPreset() {
		cameraName = "";
		optics = "";
		size = new Rectangle(0, 0);
		opticalCenter = new Rectangle(0, 0);
		a = c = e = g = i = 0.0;
	}

	public String cameraName;
	public String optics;
	public Rectangle size;
	public Rectangle opticalCenter;
	public double a, c, e, g, i;
}


public class Flatfield_ implements PlugIn
{
	protected ImagePlus image;

	// image property members
	private int width;
	private int height;
	private int opt_cent_x; // In pixels
	private int opt_cent_y; // In pixels
	
	// plugin parameters polynomial
	private double a; // 0th constant
	private double c; // 2th degree
	private double e; // 4th degree
	private double g; // 6th degree
	private double i; // 8th degree
	
	private static String propertyFile = "flatfield.properties";
	private Map<String, FlatfieldPreset> presets = new HashMap<String, FlatfieldPreset>();
	
	private TextField cameraNameField;
	private TextField opticsField;
	private TextField widthField;
	private TextField heightField;
	private TextField centerWidthField;
	private TextField centerHeightField;
	private TextField aField;
	private TextField cField;
	private TextField eField;
	private TextField gField;
	private TextField iField;
	
	private void readProperties() 
	{
		 try (InputStream input = getClass().getClassLoader().getResourceAsStream(propertyFile))
		 {
	            Properties prop = new Properties();

	            if (input == null) {
	                System.out.println("Sorry, unable to find " + propertyFile);
	                return; // add: create a data base if it's missing
	            }

	            //load a properties file from class path, inside static method
	            prop.load(input);
	            int j = 1;
	            while(true)
	            {
		            //get the property value and print it out
		            String entry = prop.getProperty("flatfield.preset." + Integer.toString(j));
		            if (entry == null) {
		            	break;
		            }
		            
		            String[] result = entry.split(",");
		            FlatfieldPreset preset = new FlatfieldPreset();
		            //for (int x=0; x < result.length; x++) {
		            	preset.cameraName = result[0];
		            	preset.optics = result[1];
		            	preset.size.width = Integer.parseInt(result[2]);
		            	preset.size.height = Integer.parseInt(result[3]);
		            	preset.opticalCenter.width = Integer.parseInt(result[4]);
		            	preset.opticalCenter.height = Integer.parseInt(result[5]);
		            	preset.a = Double.parseDouble(result[6]);
		            	preset.c = Double.parseDouble(result[7]);
		            	preset.e = Double.parseDouble(result[8]);
		            	preset.g = Double.parseDouble(result[9]);
		            	preset.i = Double.parseDouble(result[10]);
		            	
		            	System.out.println("preset no " + j + " " + result[0] + ", " + result[1] + ", " + result[2] + ", " + result[3] + ", " + result[4] + ", " + result[5] + ", " + result[6] + ", " + result[7] + ", " + result[8] + ", " + result[9] + ", " + result[10]);
		            //}
		            presets.put(preset.cameraName, preset);
		            
		            j++;
	            }
	            System.out.println("Camera no " + j + presets);

	        } catch (IOException ex) {
	            ex.printStackTrace();
        }
	 }

	
	@Override
	public void run(String value) {
		// get width, height and optical center
		
		readProperties();

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

	private boolean showDialog()
	{
		GenericDialog gd = new GenericDialog("Flatfield");
		
		// default value is 0.0000E-2, 4 digits right of the decimal point
				
		gd.addMessage("Generate flat image from polynomial");
		
		gd.addChoice("Presets: ", (String []) presets.keySet().toArray(new String[0]), "Latest");
		gd.addStringField("Optics: ", "optic",18);
		
		gd.addMessage("Fill in values or select preset.");
				
		gd.addMessage("Flat image dimensions");
		gd.addNumericField("Width:", 0, 0, 6, "pixel");
		gd.addNumericField("Height:", 0, 0, 6, "pixel");
		gd.addNumericField("Optical center X:", 0, 0, 6, "pixel");
		gd.addNumericField("Optical center Y:", 0, 0, 6, "pixel");
		
		gd.addMessage("Polynomial constants = a*r^0 + c*r^2 + e*r^4 + g*r^6 + i*r^8");
		gd.addNumericField("a = ", 0, 4, 12, "format -1.3E+12"); // should be normalized to 1 in center
		gd.addNumericField("c = ", 0, 4, 12, "");
		gd.addNumericField("e = ", 0, 4, 12, "");
		gd.addNumericField("g = ", 0, 4, 12, "");
		gd.addNumericField("i = ", 0, 4, 12, "");
				
		gd.addMessage("Choose parameters to get it normalized to =1 in center");
		gd.addMessage("Values lower than 0.3, i.e. vignetting of 70% will be cut");
		gd.addMessage("Use Excel sheet to simulate curve and calculate constants");
		gd.addMessage("www.astrofriend.eu/astronomy/tutorials");
		gd.addMessage("See AstroImageJ tutorial page 3");
		gd.addMessage(" ");
		gd.addMessage("Version 20200323");


		Choice presetChoice = (Choice)(gd.getChoices().get(0));
		presetChoice.addItemListener(new java.awt.event.ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				FlatfieldPreset preset = presets.get((String)e.getItem());
				opticsField.setText((preset.optics));
				widthField.setText(Integer.toString(preset.size.width));
				heightField.setText(Integer.toString(preset.size.height));
				centerWidthField.setText(Integer.toString(preset.opticalCenter.width));
				centerHeightField.setText(Integer.toString(preset.opticalCenter.height));
				aField.setText(Double.toString(preset.a));
				cField.setText(Double.toString(preset.c));
				eField.setText(Double.toString(preset.e));
				gField.setText(Double.toString(preset.g));
				iField.setText(Double.toString(preset.i));
			}
		});
		
		opticsField = (TextField)gd.getStringFields().get(0);
		widthField = (TextField)gd.getNumericFields().get(0);
		heightField = (TextField)gd.getNumericFields().get(1);
		centerWidthField = (TextField)gd.getNumericFields().get(2);
		centerHeightField = (TextField)gd.getNumericFields().get(3);
		aField = (TextField)gd.getNumericFields().get(4);
		cField = (TextField)gd.getNumericFields().get(5);
		eField = (TextField)gd.getNumericFields().get(6);
		gField = (TextField)gd.getNumericFields().get(7);
		iField = (TextField)gd.getNumericFields().get(8);
	
		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		// get entered values
		width = (int)gd.getNextNumber();
		height = (int)gd.getNextNumber();
		opt_cent_x = (int)gd.getNextNumber();
		opt_cent_y = (int)gd.getNextNumber();
		
		a = gd.getNextNumber();
		c = gd.getNextNumber();
		e = gd.getNextNumber();
		g = gd.getNextNumber();
		i = gd.getNextNumber();
				
		// Check calculations:
		System.out.println("A constant = " + a + ", C constant = " + c + ", E constant = " + e + ", G constant = " + g + ", I constant = " + i);
		
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
				float level = (float)(a + c*Math.pow(radie, 2) + e*Math.pow(radie, 4) + g*Math.pow(radie, 6) + i*Math.pow(radie, 8));
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
