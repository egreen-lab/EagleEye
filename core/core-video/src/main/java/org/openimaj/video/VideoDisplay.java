/**
 * Copyright (c) 2011, The University of Southampton and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openimaj.video;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.Image;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.DisplayUtilities.ImageComponent;

/**
 * Basic class for displaying videos. 
 * 
 * {@link VideoDisplayListener}s can be added to be informed when the display
 * is about to be updated or has just been updated.
 * 
 * The video can be played, paused and stopped. The difference is that during
 * pause mode, the video display events are still fired to the listeners with
 * the paused frame, whereas during stopped mode they are not. The default is
 * that when the video comes to its end, the display is automatically set to
 * stop.
 * 
 * The VideoDisplay constructor takes an {@link ImageComponent} which is used
 * to draw the video to. This allows video displays to be integrated in
 * an Swing UI. Use the {@link #createVideoDisplay(Video)} to create a basic 
 * frame displaying the video.
 * 
 * @author Sina Samangooei <ss@ecs.soton.ac.uk>
 * @author David Dupplaw <dpd@ecs.soton.ac.uk>
 *
 * @param <T> the image type of the frames in the video
 */
public class VideoDisplay<T extends Image<?,T>> implements Runnable 
{
	/**
	 *	Enumerator to represent the state of the player.
	 * 
	 * 	@author Sina Samangooei <ss@ecs.soton.ac.uk>
	 * 	@author David Dupplaw <dpd@ecs.soton.ac.uk>
	 */
	enum Mode
	{
		/** The video is playing */
		PLAY,
		
		/** The video is paused */
		PAUSE,
		
		/** The vidoe is stopped */
		STOP
	}
	
	/** The default mode is to play the player */
	private Mode mode = Mode.PLAY;
	
	/** The screen to show the player in */
	private ImageComponent screen;
	
	/** The video being displayed */
	private Video<T> video;
	
	/** The list of video display listeners */
	private List<VideoDisplayListener<T>> videoDisplayListeners;
	
	/** Whether to display the screen */
	private boolean displayMode = true;
	
	/** 
	 * Whether the video display will switch to STOP mode at the
	 * end of the video play (video.getNextFrame() returns null).
	 * Otherwise the video will be set to PAUSE.
	 */
	private boolean stopAtEndOfVideo = true;
	
	
	/**
	 * Construct a video display with the given video and frame.
	 * @param v the video
	 * @param screen the frame to draw into.
	 */
	public VideoDisplay( Video<T> v, ImageComponent screen ) 
	{
		this.video = v;
		this.screen = screen;
		videoDisplayListeners = new ArrayList<VideoDisplayListener<T>>();
	}
	
	@Override
	public void run() 
	{
		while(true)
		{
			T currentFrame = null;
			T nextFrame;
			if(this.mode == Mode.PLAY)
					nextFrame = video.getNextFrame();
			else	nextFrame = video.getCurrentFrame();
			
			// If the getNextFrame() returns null then the end of the
			// video may have been reached, so we pause the video.
			if( nextFrame == null )
				if( this.stopAtEndOfVideo )
						this.mode = Mode.STOP;
				else	this.mode = Mode.PAUSE;
			else	currentFrame = nextFrame;
			
			// If we have a frame to draw, then draw it.
			if( currentFrame != null && this.mode != Mode.STOP )
			{
				T toDraw = currentFrame.clone();
				fireBeforeUpdate(toDraw);
				if( displayMode )
					screen.setImage( ImageUtilities.createBufferedImage( toDraw ) );
				fireVideoUpdate();
				try {
					Thread.sleep(video.getMilliPerFrame());
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}
	
	/**
	 * 	Fire the event to the video listeners that a frame is about to be
	 * 	displayed on the video.
	 * 
	 *  @param currentFrame The frame that is about to be displayed
	 */
	protected void fireBeforeUpdate(T currentFrame) {
		for(VideoDisplayListener<T> vdl : videoDisplayListeners){
			vdl.beforeUpdate(currentFrame);
		}
	}

	/**
	 * 	Fire the event to the video listeners that a frame has been put on
	 * 	the display
	 */
	protected void fireVideoUpdate() {
		for(VideoDisplayListener<T> vdl : videoDisplayListeners){
			vdl.afterUpdate(this);
		}
	}

	/**
	 * Get the frame the video is being drawn to
	 * @return the frame
	 */
	public ImageComponent getScreen() {
		return screen;
	}

	/**
	 * Get the video
	 * @return the video
	 */
	public Video<T> getVideo() {
		return video;
	}

	/**
	 * Add a listener that will get fired as every
	 * frame is displayed.
	 * @param dsl the listener
	 */
	public void addVideoListener(VideoDisplayListener<T> dsl) {
		this.videoDisplayListeners.add(dsl);
	}

	/**
	 * 	Pause or resume the display. This will only have an affect if the
	 * 	video is not in STOP mode.
	 */
	public void togglePause() {
		if( this.mode == Mode.PLAY )
			this.mode = Mode.PAUSE;
		else
		if( this.mode == Mode.PAUSE )
			this.mode = Mode.PLAY;
	}
	
	/**
	 * Is the video paused?
	 * @return true if paused; false otherwise.
	 */
	public boolean isPaused() {
		return mode == Mode.PAUSE;
	}
	
	/**
	 * 	Returns whether the video is stopped or not.
	 *  @return TRUE if stopped; FALSE otherwise.
	 */
	public boolean isStopped()
	{
		return mode == Mode.STOP;
	}
	
	/**
	 * 	Whether to stop the video at the end (when {@link Video#getNextFrame()}
	 * 	returns null). If FALSE, the display will PAUSE the video; otherwise
	 * 	the video will be STOPPED.
	 * 
	 *  @param stopOnVideoEnd Whether to stop the video at the end.
	 */
	public void setStopOnVideoEnd( boolean stopOnVideoEnd )
	{
		this.stopAtEndOfVideo = stopOnVideoEnd;
	}
	
	/**
	 * Convenience function to create a VideoDisplay from an array of images
	 * @param images the images
	 * @return a VideoDisplay
	 */
	public static VideoDisplay<FImage> createVideoDisplay( FImage[] images ) 
	{
		return createVideoDisplay( new ArrayBackedVideo<FImage>(images,30) );
	}
	
	/**
	 * Convenience function to create a VideoDisplay from a video
	 * in a new window. 
	 * @param <T> the image type of the video frames 
	 * @param video the video
	 * @return a VideoDisplay
	 */
	public static<T extends Image<?,T>> VideoDisplay<T> createVideoDisplay(Video<T> video ) 
	{
		final JFrame screen = DisplayUtilities.makeFrame("Video");
		return createVideoDisplay(video,screen);
	}
	
	/**
	 * Convenience function to create a VideoDisplay from a video
	 * in a new window. 
	 * @param <T> the image type of the video frames 
	 * @param video the video
	 * @return a VideoDisplay
	 */
	public static<T extends Image<?,T>> VideoDisplay<T> createVideoDisplay(Video<T> video, JFrame screen) {
		screen.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		ImageComponent ic = new ImageComponent();
		ic.setSize( video.getWidth(), video.getHeight() );
		ic.setPreferredSize( new Dimension( video.getWidth(), video.getHeight() ) );
		screen.getContentPane().add( ic );
		
		screen.pack();
		screen.setVisible( true );
		
		VideoDisplay<T> dv = new VideoDisplay<T>( video, ic );
		
		new Thread(dv ).start();
		return dv ;
		
	}

	public void displayMode( boolean b ) 
	{
		this.displayMode  = b;
	}

}
