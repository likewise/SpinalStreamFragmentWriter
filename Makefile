# Ubuntu 20 (or similar)
# apt-get install verilator gtkwave

.ONESHELL:

.PHONY: all

all:
	set -e
	sbt "test:runMain example.StreamFragmentWriterDutSim"
	gtkwave -F -f ./simWorkspace/StreamFragmentWriterDut/test.fst -a ./StreamFragmentWriterDut.gtkw &
# continuous build/simulate on saved source code changes
# press Shift-Alt-R in GTKWave to reload waveform after code change/save/compilation
	sbt "~ test:runMain example.StreamFragmentWriterDutSim"

clean:
	rm -rf simWorkspace *.svg
