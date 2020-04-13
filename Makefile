all: compile

compile:
	cd compiler/src && $(MAKE)
	cd optimizer/src && $(MAKE)

clean:
	cd compiler/src && $(MAKE) clean
	cd optimizer/src && $(MAKE) clean
	$(RM) ./compiler/minijava-examples/minijava/*.zmips
	$(RM) ./compiler/minijava-examples/minijava/*.class
	$(RM) ./compiler/minijava-examples/minijava-error/*.zmips
	$(RM) ./compiler/minijava-examples/minijava-error/*.class
	$(RM) ./compiler/zilch-examples/*.zmips
