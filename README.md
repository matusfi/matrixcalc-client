# Matrix Calculator API test suite

The [Matrix Calculator API](http://docs.matrixcalc.apiary.io/) was created to
be a competition task. People who correctly implement the API and deploy it, 
win free [Kosice Hackathon](http://hackathon.myt-systems.sk/) tickets.

This app works as a client for the API. It tests if the server implements the
correct functionality as described in the API documentation.

It uses [test.check](https://github.com/clojure/test.check) to generate test
inputs for the API.

## Installation

#### Just the JAR

Download the latest [release](https://github.com/matusfi/matrixcalc-client/releases)
and run:

```
java -jar matrixcalc-client-0.1.0-standalone.jar http://url.of.the/api
```

#### Source Code

Clone the repo and run with [Leiningen](http://leiningen.org/):

```
git clone https://github.com/matusfi/matrixcalc-client.git
cd matrixcalc-client
lein run http://url.of.the/api
```

## Usage

```
java -jar matrixcalc-client-0.1.0-standalone.jar http://url.of.the/api
```

## Examples

A 'green' test output:

```
$ java -jar matrixcalc-client-0.1.0-standalone.jar http://matrixcalc.demecko.com/api
Tested URL: http://matrixcalc.demecko.com/api/
Testing Basic Properties => (add subtract multiply divide)
Testing Division-by-zero => (divide)
Testing Ranged operations => (sum product max min average)
Testing Whole matrix operations => (sum product max min average)
```

A 'red' test output:

```
$ java -jar matrixcalc-client-0.1.0-standalone.jar http://private-f056ad-matrixcalc.apiary-mock.com
Tested URL: http://private-f056ad-matrixcalc.apiary-mock.com/
Testing Basic Properties => (add subtract multiply divide)
FAIL: expected '0' but was '10', [http://private-f056ad-matrixcalc.apiary-mock.com/add/2-2/1-3], op: add, pos-a: 2-2, pos-b: 1-3, mtx: [[1 -2 1] [-1 -1 -1] [-2 -1 -1]]
FAIL: expected '-2' but was '10', [http://private-f056ad-matrixcalc.apiary-mock.com/add/2-3/3-2], op: add, pos-a: 2-3, pos-b: 3-2, mtx: [[1 -1 1] [-1 -1 -1] [-2 -1 -1]]
FAIL: expected '2' but was '10', [http://private-f056ad-matrixcalc.apiary-mock.com/add/1-1/1-1], op: add, pos-a: 1-1, pos-b: 1-1, mtx: [[1 -2 1] [-1 -1 -1] [-1 -1 -1]]
FAIL: expected '-2' but was '10', [http://private-f056ad-matrixcalc.apiary-mock.com/add/3-2/1-2], op: add, pos-a: 3-2, pos-b: 1-2, mtx: [[1 -1 1] [-1 -1 -1] [-1 -1 -1]]
FAIL: expected '6' but was '2', [http://private-f056ad-matrixcalc.apiary-mock.com/subtract/3-3/2-2], op: subtract, pos-a: 3-3, pos-b: 2-2, mtx: [[17 -3 -2] [-1 -1 -1] [11 -5 5]]
FAIL: expected '0' but was '2', [http://private-f056ad-matrixcalc.apiary-mock.com/subtract/2-2/2-1], op: subtract, pos-a: 2-2, pos-b: 2-1, mtx: [[9 -3 -2] [-1 -1 -1] [11 -5 5]]
...
```

## TODO

- remove unnecessary functions
- sort the remaining functions so it's easy to read the code
- add usability features
    + a nice error message when incorrect arguments are used (currently prints a stack trace, ugh)

## Known Issues

- if the API is not reachable, or it doesn't respond with JSON, all tests pass silently

## License

[Unlicensed!](http://unlicense.org)
