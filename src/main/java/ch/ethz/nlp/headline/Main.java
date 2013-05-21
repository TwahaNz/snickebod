package ch.ethz.nlp.headline;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.nlp.headline.duc2004.Duc2004Dataset;
import ch.ethz.nlp.headline.generators.BaselineGenerator;
import ch.ethz.nlp.headline.generators.CoreNLPGenerator;
import ch.ethz.nlp.headline.generators.Generator;
import ch.ethz.nlp.headline.generators.HedgeTrimmerGenerator;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

public class Main {

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);
	private static final String EVALUATION_CONFIG_FILENAME = "evaluation.conf";

	public static void main(String[] args) throws ClassNotFoundException,
			IOException {
		Dataset dataset = Duc2004Dataset.ofDefaultRoot();
		List<Task> tasks = dataset.getTasks();

		List<CoreNLPGenerator> generators = new ArrayList<>();
		generators.add(new BaselineGenerator(dataset));
		// generators.add(new SanitizingGenerator());
		// generators.add(new PosFilteredGenerator(dataset));
		// generators.add(new TfIdfWordsGenerator(dataset));
		// generators.add(new CombinedSentenceGenerator(dataset));
		generators.add(new HedgeTrimmerGenerator(dataset));

		Multimap<Task, Peer> peersMap = LinkedListMultimap.create();
		EvaluationOutput evaluationOutput = new EvaluationOutput();

		for (int i = 0; i < tasks.size(); i++) {
			Task task = tasks.get(i);
			Document document = task.getDocument();

			LOG.info(String.format("Processing task %d of %d: %s", i + 1,
					tasks.size(), document.getId()));

			for (Generator generator : generators) {
				String headline = generator.generate(document);
				Peer peer = dataset.makePeer(task, generator.getId());
				try {
					peer.store(headline);
				} catch (IOException e) {
					System.exit(-1);
					e.printStackTrace();
				}
				peersMap.put(task, peer);
			}

			evaluationOutput.log(task, peersMap.get(task));
		}

		for (CoreNLPGenerator generator : generators) {
			LOG.info(generator.getStatistics().toString());
		}

		EvaluationConfig config = new EvaluationConfig(dataset);
		Path configPath = FileSystems.getDefault().getPath(
				EVALUATION_CONFIG_FILENAME);
		config.write(configPath, peersMap);
	}
}
