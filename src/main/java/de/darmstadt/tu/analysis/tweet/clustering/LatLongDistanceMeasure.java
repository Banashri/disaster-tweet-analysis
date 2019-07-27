package de.darmstadt.tu.analysis.tweet.clustering;

import org.apache.commons.math3.ml.distance.DistanceMeasure;

public class LatLongDistanceMeasure implements DistanceMeasure {
	
	/**
	 * This method is used to find out distances in KM between two geographical
	 * locations specified in latittude-longitude
	 */
	@Override
	public double compute(double[] a, double[] b) {

		double earthRadius = 6371; //KM
	    double dLat = Math.toRadians(b[0]-a[0]);
	    double dLng = Math.toRadians(b[1]-a[1]);
	    double t = Math.sin(dLat/2) * Math.sin(dLat/2) +
	               Math.cos(Math.toRadians(a[0])) * Math.cos(Math.toRadians(b[0])) *
	               Math.sin(dLng/2) * Math.sin(dLng/2);
	    double c = 2 * Math.atan2(Math.sqrt(t), Math.sqrt(1-t));
	    double dist = earthRadius * c;

	    return dist;
	}
}
