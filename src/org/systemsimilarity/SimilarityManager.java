package org.systemsimilarity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.designroleminer.ClassMetricResult;
import org.designroleminer.MetricReport;
import org.threshold.DoseaReferenceTechnique;
import org.threshold.TechniqueExecutor;

public class SimilarityManager {

	public ArrayList<String> filterSimilarProjects(String project, List<SimilarityResult> listSimilarity,
			double threshold) {
		ArrayList<String> filteredProjects = new ArrayList<String>();
		for (SimilarityResult result : listSimilarity) {
			if (result.getProject1().equals(project)) {
				if (result.getSimilarity() >= threshold) {
					filteredProjects.add(result.getProject2());
				}
			}
		}
		return filteredProjects;
	}

	public List<SimilarityResult> calculate(ArrayList<String> projetosReferencia, String pastaMetricas) {

		Map<String, MetricReport> mapProjetosMetricas = new HashMap<String, MetricReport>();
		TechniqueExecutor executor = new TechniqueExecutor();

		ArrayList<String> listaUnicoProjeto;
		for (String projeto : projetosReferencia) {
			listaUnicoProjeto = new ArrayList<String>();
			listaUnicoProjeto.add(projeto);
			mapProjetosMetricas.put(projeto, executor.getMetricsFromProjects(listaUnicoProjeto, pastaMetricas, ""));
		}

		List<SimilarityResult> listSimilarityResult = new ArrayList<SimilarityResult>();
		Iterator<String> it1 = mapProjetosMetricas.keySet().iterator();
		Iterator<String> it2;
		while (it1.hasNext()) {
			String projeto1 = it1.next();
			it2 = mapProjetosMetricas.keySet().iterator();
			MetricReport metricasProjeto1 = mapProjetosMetricas.get(projeto1);
			MetricReport metricasProjeto2;
			while (it2.hasNext()) {
				String projeto2 = it2.next();
				metricasProjeto2 = mapProjetosMetricas.get(projeto2);
				Double similarity = calculate(metricasProjeto1.all(), metricasProjeto2.all());
				SimilarityResult result = new SimilarityResult();
				result.setProject1(projeto1);
				result.setProject2(projeto2);
				result.setSimilarity(similarity);
				listSimilarityResult.add(result);
			}
		}
		return listSimilarityResult;
	}

	public Double calculate(Collection<ClassMetricResult> projeto1, Collection<ClassMetricResult> projeto2) {

		DoseaReferenceTechnique designRoleTechinique = new DoseaReferenceTechnique();

		HashMap<String, Long> linhasDeCodigoPorDesignRoleProjeto1 = new HashMap<String, Long>();
		Long totalLocProjeto1 = designRoleTechinique.obterTotalLinhasCodigoPorDesignRole(projeto1,
				linhasDeCodigoPorDesignRoleProjeto1);

		if (linhasDeCodigoPorDesignRoleProjeto1.get("TEST") != null)
			totalLocProjeto1 -= linhasDeCodigoPorDesignRoleProjeto1.remove("TEST");
		if (linhasDeCodigoPorDesignRoleProjeto1.get("UNDEFINED") != null)
			totalLocProjeto1 -= linhasDeCodigoPorDesignRoleProjeto1.remove("UNDEFINED");
		if (linhasDeCodigoPorDesignRoleProjeto1.get("ENTITY") != null)
			totalLocProjeto1 -= linhasDeCodigoPorDesignRoleProjeto1.remove("ENTITY");

		HashMap<String, Long> linhasDeCodigoPorDesignRoleProjeto2 = new HashMap<String, Long>();

		Long totalLocProjeto2 = designRoleTechinique.obterTotalLinhasCodigoPorDesignRole(projeto2,
				linhasDeCodigoPorDesignRoleProjeto2);
		if (linhasDeCodigoPorDesignRoleProjeto2.get("TEST") != null)
			totalLocProjeto2 -= linhasDeCodigoPorDesignRoleProjeto2.remove("TEST");
		if (linhasDeCodigoPorDesignRoleProjeto2.get("UNDEFINED") != null)
			totalLocProjeto2 -= linhasDeCodigoPorDesignRoleProjeto2.remove("UNDEFINED");
		if (linhasDeCodigoPorDesignRoleProjeto2.get("ENTITY") != null)
			totalLocProjeto2 -= linhasDeCodigoPorDesignRoleProjeto2.remove("ENTITY");

		Map<CharSequence, Double> drProjetoPercentual1 = new HashMap<CharSequence, Double>();
		Map<CharSequence, Double> drProjetoPercentual2 = new HashMap<CharSequence, Double>();

		for (CharSequence designRole : linhasDeCodigoPorDesignRoleProjeto1.keySet()) {
			if (!designRole.equals("TEST") && !designRole.equals("ENTITY") && !designRole.equals("UNDEFINED")) {
				Double valor = ((double) linhasDeCodigoPorDesignRoleProjeto1.get(designRole) / totalLocProjeto1) * 100;
				drProjetoPercentual1.put(designRole, valor);
			}
		}

		for (CharSequence designRole : linhasDeCodigoPorDesignRoleProjeto2.keySet()) {
			if (!designRole.equals("TEST") && !designRole.equals("ENTITY") && !designRole.equals("UNDEFINED")) {
				Double valor = ((double) linhasDeCodigoPorDesignRoleProjeto2.get(designRole) / totalLocProjeto2) * 100;
				drProjetoPercentual2.put(designRole, valor);
			}
		}

		return cosineSimilarity(drProjetoPercentual1, drProjetoPercentual2);
	}

	/**
	 * Calculates the cosine similarity for two given vectors.
	 *
	 * @param leftVector  left vector
	 * @param rightVector right vector
	 * @return cosine similarity between the two vectors
	 */
	private Double cosineSimilarity(final Map<CharSequence, Double> leftVector,
			final Map<CharSequence, Double> rightVector) {
		if (leftVector == null || rightVector == null) {
			throw new IllegalArgumentException("Vectors must not be null");
		}

		final Set<CharSequence> intersection = getIntersection(leftVector, rightVector);

		final double dotProduct = dot(leftVector, rightVector, intersection);
		double d1 = 0.0d;
		for (final Double value : leftVector.values()) {
			d1 += Math.pow(value, 2);
		}
		double d2 = 0.0d;
		for (final Double value : rightVector.values()) {
			d2 += Math.pow(value, 2);
		}
		double cosineSimilarity;
		if (d1 <= 0.0 || d2 <= 0.0) {
			cosineSimilarity = 0.0;
		} else {
			cosineSimilarity = (double) (dotProduct / (double) (Math.sqrt(d1) * Math.sqrt(d2)));
		}
		return cosineSimilarity;
	}

	/**
	 * Returns a set with strings common to the two given maps.
	 * 
	 * @param leftVector  left vector map
	 * @param rightVector right vector map
	 * @return common strings
	 */
	private Set<CharSequence> getIntersection(final Map<CharSequence, Double> leftVector,
			final Map<CharSequence, Double> rightVector) {
		final Set<CharSequence> intersection = new HashSet<CharSequence>(leftVector.keySet());
		intersection.retainAll(rightVector.keySet());
		return intersection;
	}

	/**
	 * Computes the dot product of two vectors. It ignores remaining elements. It
	 * means that if a vector is longer than other, then a smaller part of it will
	 * be used to compute the dot product.
	 * 
	 * @param leftVector   left vector
	 * @param rightVector  right vector
	 * @param intersection common elements
	 * @return the dot product
	 */
	private double dot(final Map<CharSequence, Double> leftVector, final Map<CharSequence, Double> rightVector,
			final Set<CharSequence> intersection) {
		long dotProduct = 0;
		for (final CharSequence key : intersection) {
			dotProduct += leftVector.get(key) * rightVector.get(key);
		}
		return dotProduct;
	}

}
