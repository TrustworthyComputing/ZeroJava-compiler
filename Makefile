all: compile

compile:
	cd compiler/src/zilch && $(MAKE)
	cd optimizer/src/zilch && $(MAKE)

clean:
	cd compiler/src/zilch && $(MAKE) clean
	cd optimizer/src/zilch && $(MAKE) clean
	rm ./compiler/zilch-examples/*.zmips
