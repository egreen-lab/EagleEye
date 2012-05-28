package org.openimaj.image.processing.face.tracking.clm;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processing.resize.ResizeProcessor;
import org.openimaj.image.processing.transform.PiecewiseMeshWarp;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Shape;
import org.openimaj.math.geometry.shape.Triangle;
import org.openimaj.util.pair.Pair;
import org.openimaj.video.capture.VideoCapture;

import Jama.Matrix;

public class Puppeteer {
	public static void main(String[] args) throws Exception {

		boolean fcheck = true; 
		double scale = 1; 
		int fpd = -1; 
		boolean show = true;

		//set other tracking parameters
		int [] wSize1 = {7};
		int [] wSize2 = {11, 9, 7};

		int nIter = 5; 
		double clamp=3;
		double fTol=0.01;

		final Tracker model = Tracker.Load("/Users/jsh2/Desktop/FaceTracker/model/face2.tracker");
		int [][] tri = IO.LoadTri("/Users/jsh2/Desktop/FaceTracker/model/face.tri");
		int [][] con = IO.LoadCon("/Users/jsh2/Desktop/FaceTracker/model/face.con");

		//initialize camera and display window
		VideoCapture vc = new VideoCapture(320, 240);

		JFrame jfr = DisplayUtilities.displayName(vc.getNextFrame(), "Face Tracker");
		jfr.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyChar() == 'd')
					model.FrameReset();
			}
		});

		
		MBFImage puppet = ImageUtilities.readMBF(new URL("http://www.oii.ox.ac.uk/images/people/large/nigel_shadbolt.jpg"));
		FImage pimg = puppet.flatten();
		if (model.Track(pimg, wSize2, fpd, nIter, clamp, fTol, false) != 0) throw new Exception("puppet not found");
		List<Triangle> puppetTris = getTriangles(model._shape, con, tri, model._clm._visi[model._clm.GetViewIdx()]);
		model.FrameReset();
		
		
		boolean failed = true; 
		while (true) 
		{
			//grab image, resize and flip
			MBFImage frame = vc.getNextFrame();
			//			MBFImage frame = ImageUtilities.readMBF(new File("/Users/jsh2/Desktop/face.png"));
			FImage im = frame.flatten();
			//			im.multiplyInline(255F);

			if(scale != 1)
				im = ResizeProcessor.resample(im, (int)(scale*im.width), (int)(scale*im.height));

			//flip image?

			//track this image
			int[] wSize; 
			if (failed)
				wSize = wSize2; 
			else 
				wSize = wSize1;

			if ( model.Track(im, wSize, fpd, nIter, clamp, fTol, fcheck) == 0 ) {
				int idx = model._clm.GetViewIdx();
				failed = false;

				System.out.println("tracked");
				
				List<Pair<Shape>> mtris = new ArrayList<Pair<Shape>>(); 
				List<Triangle> tris = getTriangles(model._shape, con, tri, model._clm._visi[idx]);
				for (int i=0; i<tris.size(); i++) {
					Triangle t1 = puppetTris.get(i);
					Triangle t2 = tris.get(i);
					if (t1 != null && t2 != null) {
						mtris.add(new Pair<Shape>(t1, t2));
					}
				}
				
				PiecewiseMeshWarp<Float[], MBFImage> pmw = new PiecewiseMeshWarp<Float[], MBFImage>(mtris);
				MBFImage tmp = puppet.process(pmw);
				for (int y=0; y<Math.min(frame.getHeight(), tmp.getHeight()); y++)
					for (int x=0; x<Math.min(frame.getWidth(), tmp.getWidth()); x++)
						if (tmp.bands.get(0).pixels[y][x] != 0 && tmp.bands.get(1).pixels[y][x] != 0 && tmp.bands.get(2).pixels[y][x] != 0)
							frame.setPixel(x, y, tmp.getPixel(x, y));
							
			} else {
				System.out.println("failed");
				model.FrameReset();
				failed = true;
			}     

			//show image and check for user input
			DisplayUtilities.displayName(frame, "Face Tracker");
		}
	}

	static List<Triangle> getTriangles(Matrix shape, int[][] con, int[][] tri, Matrix visi)
	{
		final int n = shape.getRowDimension() / 2; 
		List<Triangle> tris = new ArrayList<Triangle>();

		//draw triangulation
		for (int i = 0; i < tri.length; i++) {
			if (visi.get(tri[i][0], 0) == 0 ||
					visi.get(tri[i][1],0) == 0 ||
					visi.get(tri[i][2],0) == 0) 
			{
				tris.add(null);
			} else {
				Triangle t = new Triangle(
						new Point2dImpl((float)shape.get(tri[i][0],0), (float)shape.get(tri[i][0]+n,0)),
						new Point2dImpl((float)shape.get(tri[i][1],0), (float)shape.get(tri[i][1]+n,0)),
						new Point2dImpl((float)shape.get(tri[i][2],0), (float)shape.get(tri[i][2]+n,0))
				);
				tris.add(t);
			}
		}
		
		return tris;
	}
}