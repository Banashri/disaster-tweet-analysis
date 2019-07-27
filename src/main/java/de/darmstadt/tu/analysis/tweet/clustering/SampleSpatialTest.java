package de.darmstadt.tu.analysis.tweet.clustering;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.context.SpatialContextFactory;
import com.spatial4j.core.context.jts.JtsSpatialContext;
import com.spatial4j.core.context.jts.JtsSpatialContextFactory;
import com.spatial4j.core.shape.Shape;
import com.spatial4j.core.shape.SpatialRelation;
import com.spatial4j.core.shape.jts.JtsGeometry;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.IntersectionMatrix;

public class SampleSpatialTest {

	public static void main(String[] args) throws ParseException {
		try {
			final SpatialContextFactory spatialContextFactory = new SpatialContextFactory();
			final JtsSpatialContextFactory jtsCtxFactory =  new JtsSpatialContextFactory();
			SpatialContext ctx = spatialContextFactory.newSpatialContext();
			JtsSpatialContext jtsCtx = jtsCtxFactory.newSpatialContext();

			/**
			 * Shapes obtained after reading the file
			 */
			
			final String POLY_1 = "Polygon((10 10, 40 10, 40 30, 10 30, 10 10))";
			final String POINT_1 = "POINT(35 5)"; 
			final String LINE_1 = "LINESTRING(-20 30, 20 -10)";
			
			Set<String> existingShapesSet = new HashSet<String>();
			existingShapesSet.add(LINE_1);
			existingShapesSet.add(POINT_1);
			existingShapesSet.add(POLY_1);
			
			List<Shape> existingShapes = new ArrayList<Shape>();
			existingShapes.add(jtsCtx.readShapeFromWkt(POLY_1));
			existingShapes.add(jtsCtx.readShapeFromWkt(LINE_1));
			existingShapes.add(jtsCtx.readShapeFromWkt(POINT_1));

			final String POLY_2 = "Polygon((20 -10, 50 -10, 50 20, 20 20, 20 -10))";
			final String POLY_3 = "Polygon((0 10, 20 -10, 20 20, 0 10))";
			final String POINT_2 = "POINT(15 5)"; //test
			
			List<Shape> testShapes = new ArrayList<Shape>();
			testShapes.add(jtsCtx.readShapeFromWkt(POLY_2));
			testShapes.add(jtsCtx.readShapeFromWkt(POLY_3));
			testShapes.add(jtsCtx.readShapeFromWkt(POINT_2));

			Set<String> setOfAllNonClusteredPoints = new HashSet<String>();
			List listOfPointsForIntersectedShapes = new ArrayList();
		
			Map<Shape, List<Shape>> intersectingShapes = new HashMap<Shape, List<Shape>>();
			
			for (Shape s: testShapes) {
				for (Shape es: existingShapes) {

					IntersectionMatrix expectedM = null;
					
					expectedM = jtsCtx.getGeometryFrom(es).relate(jtsCtx.getGeometryFrom(s));
					SpatialRelation expectedSR = JtsGeometry.intersectionMatrixToSpatialRelation(expectedM);

					if (expectedSR != SpatialRelation.DISJOINT) {
						expectedSR = SpatialRelation.CONTAINS;
						
						System.out.println("Intersection between "+es + " and "+s+ " is :"+expectedSR);
						
						if (intersectingShapes.containsKey(s)) {
							List<Shape> shapes = intersectingShapes.get(s);
							shapes.add(es);
							intersectingShapes.put(s, shapes);
						} else {
							List<Shape> shapes = new ArrayList<Shape>();
							shapes.add(es);
							intersectingShapes.put(s, shapes);
						}
					} else {
						System.out.println("NO Intersection between "+es + " and "+s+ " is :"+expectedSR);
						if (!intersectingShapes.containsKey(s)) {
							List<Shape> shapes = new ArrayList<Shape>();
							intersectingShapes.put(s, shapes);
						}
					}
				}
			}
			System.out.println("intersectingShapes" + intersectingShapes.size());

			for (Map.Entry<Shape, List<Shape>> entry: intersectingShapes.entrySet()) {

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Set<String> getCoordinatesFromIntersectingShapes(
			JtsGeometry es, JtsGeometry s) {

		Set<String> coordinates = new HashSet<String>();

		for (Coordinate ec: es.getGeom().getCoordinates()) {
			coordinates.add(ec.x+" "+ec.y);
		}
		for (Coordinate c: s.getGeom().getCoordinates()) {
			coordinates.add(c.x+" "+c.y);
		}
		return coordinates;
	}
}