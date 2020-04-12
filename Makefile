all: compile

compile:
	cd compiler/src && $(MAKE)
	cd optimizer/src && $(MAKE)

clean:
	cd compiler/src && $(MAKE) clean
	cd optimizer/src && $(MAKE) clean
	$(RM) ./compiler/zilch-examples/*.zmips
