package tutorials.utils;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;

import java.util.List;
import java.util.Map;

public class LineChart extends ApplicationFrame {
	private static final long serialVersionUID = 1L;

	public LineChart(String applicationTitle, String chartTitle, String sub, List<DataSetTrec> t, char type) {
		super(applicationTitle);
		JFreeChart lineChart = null;
		switch (type) {
		case 'p':
			lineChart = ChartFactory.createLineChart(chartTitle, "Docs", "Precision", createDatasetPages(t),
					PlotOrientation.VERTICAL, true, true, false);
			break;
		case 'r':
			lineChart = ChartFactory.createLineChart(chartTitle, "Recall", "Precision", createDatasetRecalls(t),
					PlotOrientation.VERTICAL, true, true, false);
			break;

		default:
			break;
		}

		lineChart.addSubtitle(new TextTitle(sub + " query results"));

		ChartPanel chartPanel = new ChartPanel(lineChart);
		chartPanel.setPreferredSize(new java.awt.Dimension(560, 367));
		setContentPane(chartPanel);
	}

	private DefaultCategoryDataset createDatasetPages(List<DataSetTrec> t) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		for (DataSetTrec trec : t) {
			for (Map.Entry<String, Float> entry : trec.getPrecisions().entrySet()) {

				dataset.addValue(entry.getValue(), trec.getName(), entry.getKey());
			}
		}

		return dataset;
	}

	private DefaultCategoryDataset createDatasetRecalls(List<DataSetTrec> t) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		for (DataSetTrec trec : t) {
			for (Map.Entry<String, Float> entry : trec.getRecalls().entrySet()) {

				dataset.addValue(entry.getValue(), trec.getName(), entry.getKey());
			}
		}

		return dataset;
	}

}
