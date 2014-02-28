SearchEngine
============

Search engine for collection of CACM text files which utilizes the Lucene open source library's tokenizer, stopword filters, and stemming filters to test out various information retrieval techniques to improve quality of results.

Techniques used include clustering based on MI, EMI, dice and chi-square association measures, Vector Space Model based on a cosine similarity measure with generic tf-idf vectors, stop word filtering, stemming using Porter Stemming algorithm, and query expansion techniques.

Documents contained include over three thousand short CACM text files, a few text files containing stop words, a file containing approximately 50 queries, a file containing relevant document judgements for those queries, and then files for the actual search engine.

Functionality has not been extended yet to generalize this search engine to any set of text files. 
Functionality to be added: Allow user to input custom query through console.

Primary purpose of this project was to understand the effects of different Information Retrieval techniques on the precision of a search engine over a set of multiple queries for a large corpus.
