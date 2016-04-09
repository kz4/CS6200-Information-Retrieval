package com.ir.plot;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import com.ir.util.Constants;


public class XYSeriesDemo extends ApplicationFrame {

	/**
	 * A demonstration application showing an XY series containing a null value.
	 *
	 * @param title  the frame title.
	 * @throws IOException 
	 */
	public XYSeriesDemo(final String title) throws IOException {

		super(title);
//		final XYSeries series = new XYSeries("Random Data");
//		series.add(1.0, 500.2);
//		series.add(5.0, 694.1);
//		series.add(4.0, 100.0);
//		series.add(12.5, 734.4);
//		series.add(17.3, 453.2);
//		series.add(21.2, 500.2);
//		series.add(21.9, null);
//		series.add(25.6, 734.4);
//		series.add(30.0, 453.2);
//		final XYSeries series2 = new XYSeries("Random Data2");
//		series2.add(1, 250);
//		series2.add(5, 250);
//		series2.add(10, 250);
//		series2.add(15, 250);
//		series2.add(20, 250);
//		series2.add(25, 250);
//		series2.add(30, 250);
		
		final XYSeries series = new XYSeries("Precision Recall Curve");
		final XYSeries series2 = new XYSeries("Interpolated Precision Recall Curve");
		read_prec_recall_data(series, series2);
		
		final XYSeriesCollection data = new XYSeriesCollection();
		data.addSeries(series);
		data.addSeries(series2);
		final JFreeChart chart = ChartFactory.createXYLineChart(
				"XY Series Demo",
				"X", 
				"Y", 
				data,
				PlotOrientation.VERTICAL,
				true,
				true,
				false
				);

		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		setContentPane(chartPanel);

	}

	//****************************************************************************
	//* JFREECHART DEVELOPER GUIDE                                               *
	//* The JFreeChart Developer Guide, written by David Gilbert, is available   *
	//* to purchase from Object Refinery Limited:                                *
	//*                                                                          *
	//* http://www.object-refinery.com/jfreechart/guide.html                     *
	//*                                                                          *
	//* Sales are used to provide funding for the JFreeChart project - please    * 
	//* support us so that we can continue developing free software.             *
	//****************************************************************************

	private void read_prec_recall_data(XYSeries series, XYSeries series2) throws IOException {
		try(BufferedReader br = new BufferedReader(new FileReader(Constants.prec_recall_curves))) {
//			String line = br.readLine();

//			while (line != null) {
//				String[]              
//				line = br.readLine();
//			}
			
			String line = br.readLine();
			String[] x = line.split(" ");
			line = br.readLine();
			String[] y = line.split(" ");
			line = br.readLine();
			String[] y_interpolated = line.split(" ");
			
			for (int i = 0; i < y_interpolated.length; i++) {
				series.add(Double.parseDouble(x[i]), Double.parseDouble(y[i]));
				series2.add(Double.parseDouble(x[i]), Double.parseDouble(y_interpolated[i]));
				
			}
		}
	}

	/**
	 * Starting point for the demonstration application.
	 *
	 * @param args  ignored.
	 * @throws IOException 
	 */
	public static void main(final String[] args) throws IOException {

		final XYSeriesDemo demo = new XYSeriesDemo("Precision-Recall Curve - " + Constants.query);
		demo.pack();
		RefineryUtilities.centerFrameOnScreen(demo);
		demo.setVisible(true);
//		final XYSeriesDemo demo2 = new XYSeriesDemo("Precision-Recall Curve");
//		demo2.pack();
//		RefineryUtilities.centerFrameOnScreen(demo);
//		demo2.setVisible(true);
//		final XYSeriesDemo demo3 = new XYSeriesDemo("Precision-Recall Curve");
//		demo3.pack();
//		RefineryUtilities.centerFrameOnScreen(demo);
//		demo3.setVisible(true);

	}

}