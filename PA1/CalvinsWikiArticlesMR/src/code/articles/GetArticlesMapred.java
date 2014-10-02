package code.articles;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.filecache.DistributedCache;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import edu.umd.cloud9.collection.wikipedia.WikipediaPage;
import util.WikipediaPageInputFormat;

/**
 * This class is used for Section A of assignment 1. You are supposed to
 * implement a main method that has first argument to be the dump wikipedia
 * input filename , and second argument being an output filename that only
 * contains articles of people as mentioned in the people auxiliary file.
 */
public class GetArticlesMapred {

	//@formatter:off
	/**
	 * Input:
	 * 		Page offset 	WikipediaPage
	 * Output
	 * 		Page offset 	WikipediaPage
	 * @author Tuan
	 *
	 */
	//@formatter:on
	public static class GetArticlesMapper extends Mapper<LongWritable, WikipediaPage, Text, Text> {
		public static Set<String> peopleArticlesTitles = new HashSet<String>();

		@Override
		protected void setup(Mapper<LongWritable, WikipediaPage, Text, Text>.Context context)
				throws IOException, InterruptedException {
			// TODO: You should implement people articles load from
			// DistributedCache here
			//super.setup(context);
			
			URI[] uris = DistributedCache.getCacheFiles(context.getConfiguration());
			
			BufferedReader bReader = new BufferedReader(new FileReader(uris[0].toString()));
			String line;
			while ((line = bReader.readLine()) != null) {
				peopleArticlesTitles.add(line);
			}
			bReader.close();

		}

		@Override
		public void map(LongWritable offset, WikipediaPage inputPage, Context context)
				throws IOException, InterruptedException {
			// TODO: You should implement getting article mapper here
			
			if (peopleArticlesTitles.contains(inputPage.getTitle())) {
				Text articleXML = new Text(inputPage.getRawXML());
				context.write(new Text(), articleXML);  
			}
			
		}
	}

	public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException, ClassNotFoundException {
		// TODO: you should implement the Job Configuration and Job call
		// here

		Job job = Job.getInstance(new Configuration());
		job.addCacheFile(new URI("people.txt"));
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		job.setMapperClass(GetArticlesMapper.class); 
		//job.setReducerClass(SumReducer.class);  
		
		job.setInputFormatClass(WikipediaPageInputFormat.class);
		
		job.setOutputFormatClass(TextOutputFormat.class);
		
		FileInputFormat.setInputPaths(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		
		job.setJarByClass(GetArticlesMapred.class);
		
		job.submit();

	}
}
