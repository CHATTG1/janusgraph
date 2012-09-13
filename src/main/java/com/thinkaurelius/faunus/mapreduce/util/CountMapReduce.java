package com.thinkaurelius.faunus.mapreduce.util;

import com.thinkaurelius.faunus.FaunusEdge;
import com.thinkaurelius.faunus.FaunusVertex;
import com.thinkaurelius.faunus.Tokens;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class CountMapReduce {

    public static final String CLASS = Tokens.makeNamespace(CountMapReduce.class) + ".class";

    public enum Counters {
        VERTICES_COUNTED,
        EDGES_COUNTED
    }

    public static class Map extends Mapper<NullWritable, FaunusVertex, IntWritable, LongWritable> {

        private final static IntWritable intWritable = new IntWritable(1);
        private boolean isVertex;
        private final LongWritable longWritable = new LongWritable();

        @Override
        public void setup(final Mapper.Context context) throws IOException, InterruptedException {
            this.isVertex = context.getConfiguration().getClass(CLASS, Element.class, Element.class).equals(Vertex.class);
        }

        @Override
        public void map(final NullWritable key, final FaunusVertex value, final Mapper<NullWritable, FaunusVertex, IntWritable, LongWritable>.Context context) throws IOException, InterruptedException {

            if (this.isVertex) {
                if (value.hasPaths()) {
                    this.longWritable.set(value.pathCount());
                    context.write(intWritable, this.longWritable);
                }
            } else {
                long pathCount = 0;
                for (final Edge e : value.getEdges(Direction.OUT)) {
                    final FaunusEdge edge = (FaunusEdge) e;
                    if (edge.hasPaths()) {
                        pathCount = pathCount + edge.pathCount();
                    }
                }
                if (pathCount > 0) {
                    this.longWritable.set(pathCount);
                    context.write(intWritable, this.longWritable);
                }
            }
        }
    }

    public static class Combiner extends Reducer<IntWritable, LongWritable, IntWritable, LongWritable> {

        private final LongWritable longWritable = new LongWritable();

        @Override
        public void reduce(final IntWritable key, final Iterable<LongWritable> values, final Reducer<IntWritable, LongWritable, IntWritable, LongWritable>.Context context) throws IOException, InterruptedException {
            long totalCount = 0;
            for (final LongWritable temp : values) {
                totalCount = totalCount + temp.get();
            }
            this.longWritable.set(totalCount);
            context.write(key, this.longWritable);
        }
    }

    public static class Reduce extends Reducer<IntWritable, LongWritable, NullWritable, Text> {

        @Override
        public void reduce(final IntWritable key, final Iterable<LongWritable> values, final Reducer<IntWritable, LongWritable, NullWritable, Text>.Context context) throws IOException, InterruptedException {
            long totalCount = 0;
            for (final LongWritable temp : values) {
                totalCount = totalCount + temp.get();
            }
            context.write(NullWritable.get(), new Text(String.valueOf(totalCount)));
        }
    }
}
