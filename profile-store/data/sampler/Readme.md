# LFM-1b-sample

* Small subset of http://www.cp.jku.at/datasets/LFM-1b/ for testing and demo
* As the original dataset is quite huge, a subset can be sampled using the `lfm_sampler.py`

## lfm_sampler.py

* samples from the original dataset by only reading the head of the events file and then retrieving the required ids for artists, albums & tracks
* In small samples, users might have only few shared listening events, which can be a problem for the recommendation.
 Connectivity can be controlled by the m`min-avg-listeners` option.
* Requirements: pyton>=3.6, pandas>=1.0
* for usage hints, execute `python lfm_sampler.py -h`
