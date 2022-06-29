# LFM-1b-sample

Since the [original dataset]( http://www.cp.jku.at/datasets/LFM-1b/) is very large, we use
a small connected subset for testing and demo.

## sampler

Samples from the original dataset by only reading the head of the events file
and then retrieving the required ids for artists, albums and tracks. 

In small samples, users might have only few shared listening events,
which can be a problem for the recommendation algorithm.
Connectivity can be controlled by the `min-avg-listeners` option.

Requires `python >= 3.6`, `pandas >= 1.0`.
Implements a cli, so `python lfm_sampler.py -h` reports usage and options.
