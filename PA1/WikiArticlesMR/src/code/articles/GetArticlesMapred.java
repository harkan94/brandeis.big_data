package code.articles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import util.WikipediaPageInputFormat;
import edu.umd.cloud9.collection.wikipedia.WikipediaPage;

/**
 * This class is used for Section A of assignment 1. You are supposed to run the
 * code taking the wikipedia dump file as input, and output being the raw XML of
 * Wikipedia articles that matches the people.txt list.
 * 
 * @author Calvin Wang, minwang@brandeis.edu
 */
public class GetArticlesMapred {

	public static class GetArticlesMapper extends Mapper<LongWritable, WikipediaPage, Text, Text> {

		// used to store people names to match up with Wikipedia articles
		public static Set<String> peopleList = new HashSet<String>();

		@Override
		protected void setup(Mapper<LongWritable, WikipediaPage, Text, Text>.Context context)
				throws IOException, InterruptedException {

			File file = new File("people.txt");
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			// loop used to add each line from people.txt into peopleList
			while ((line = br.readLine()) != null) {
				peopleList.add(line);
			}
			br.close();
		}

		@Override
		public void map(LongWritable offset, WikipediaPage inputPage, Context context)
				throws IOException, InterruptedException {

			// conditional to take out the articles that have titles matching in
			// peopleList
			if (peopleList.contains(inputPage.getTitle())) {
				Text articleXML = new Text(inputPage.getRawXML());
				context.write(new Text(), articleXML);
			}
		}
	}

	public static void main(String[] args) throws IOException, URISyntaxException,
			InterruptedException, ClassNotFoundException {

		Job job = Job.getInstance(new Configuration());

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		job.setMapperClass(GetArticlesMapper.class);

		job.setInputFormatClass(WikipediaPageInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);

		FileInputFormat.setInputPaths(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		job.setJarByClass(GetArticlesMapred.class);

		// so we don't have to specify the job name when starting job on cluster
		job.getConfiguration().set("mapreduce.job.queuename", "hadoop08");

		job.submit();

	}
}
