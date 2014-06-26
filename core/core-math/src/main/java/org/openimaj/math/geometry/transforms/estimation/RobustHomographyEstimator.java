package org.openimaj.math.geometry.transforms.estimation;

import java.util.List;

import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.transforms.HomographyModel;
import org.openimaj.math.geometry.transforms.HomographyRefinement;
import org.openimaj.math.geometry.transforms.TransformUtilities;
import org.openimaj.math.geometry.transforms.residuals.AlgebraicResidual2d;
import org.openimaj.math.model.fit.LMedS;
import org.openimaj.math.model.fit.RANSAC;
import org.openimaj.math.model.fit.RANSAC.StoppingCondition;
import org.openimaj.math.model.fit.RobustModelFitting;
import org.openimaj.util.pair.IndependentPair;

import Jama.Matrix;

/**
 * Helper class to simplify robust estimation of homographies without having to
 * deal with the nuts and bolts of the underlying robust model fitters, etc. The
 * overall robust estimation process is as follows:
 * <p>
 * An initial estimate of the inliers and an algebraically optimal homography is
 * computed using {@link RANSAC} or {@link LMedS}. In both cases, the normalised
 * DLT algorithm is used (see {@link TransformUtilities#homographyMatrix(List)}
 * <p>
 * If an reasonable initial estimate was found, non-linear optimisation using
 * Levenburg-Marquardt is performed to on the inliers using the initial estimate
 * to optimise against a true geometric residual given by
 * {@link HomographyRefinement}.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 */
public class RobustHomographyEstimator implements RobustModelFitting<Point2d, Point2d, HomographyModel> {
	private RobustModelFitting<Point2d, Point2d, HomographyModel> robustFitter;
	private HomographyRefinement refinement;

	/**
	 * Construct using the {@link LMedS} algorithm with the given expected
	 * outlier percentage
	 * 
	 * @param outlierProportion
	 *            expected proportion of outliers (between 0 and 1)
	 * @param refinement
	 *            the refinement technique
	 */
	public RobustHomographyEstimator(double outlierProportion, HomographyRefinement refinement) {
		robustFitter = new LMedS<Point2d, Point2d, HomographyModel>(
				new HomographyModel(),
				new AlgebraicResidual2d<HomographyModel>(),
				outlierProportion, true);

		this.refinement = refinement;
	}

	/**
	 * Construct using the {@link RANSAC} algorithm with the given options.
	 * 
	 * @param threshold
	 *            the threshold on the {@link AlgebraicResidual2d} at which to
	 *            consider a point as an inlier
	 * @param nIterations
	 *            the maximum number of iterations
	 * @param stoppingCondition
	 *            the {@link StoppingCondition} for RANSAC
	 * @param refinement
	 *            the refinement technique
	 */
	public RobustHomographyEstimator(double threshold, int nIterations, StoppingCondition stoppingCondition,
			HomographyRefinement refinement)
	{
		robustFitter = new RANSAC<Point2d, Point2d, HomographyModel>(new HomographyModel(),
				new AlgebraicResidual2d<HomographyModel>(), threshold, nIterations, stoppingCondition, true);

		this.refinement = refinement;
	}

	@Override
	public boolean fitData(List<? extends IndependentPair<Point2d, Point2d>> data) {
		// Use a robust fitting technique to find the inliers and estimate a
		// model using DLT
		if (!robustFitter.fitData(data)) {
			// just go with full-on DLT estimate rather than a robust one
			robustFitter.getModel().estimate(data);

			return false;
		}

		// Now apply non-linear optimisation to get a better estimate
		final Matrix optimised = refinement.refine(robustFitter.getModel().getTransform(), robustFitter.getInliers());
		robustFitter.getModel().setTransform(optimised);

		return true;
	}

	@Override
	public int numItemsToEstimate() {
		return robustFitter.numItemsToEstimate();
	}

	@Override
	public HomographyModel getModel() {
		return robustFitter.getModel();
	}

	@Override
	public List<? extends IndependentPair<Point2d, Point2d>> getInliers() {
		return robustFitter.getInliers();
	}

	@Override
	public List<? extends IndependentPair<Point2d, Point2d>> getOutliers() {
		return robustFitter.getOutliers();
	}
}