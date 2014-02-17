package psicat.util

import groovy.util.GroovyTestCase
import psicat.util.CustomVerifier

class CustomVerifierTests extends GroovyTestCase {
	public void testVerifier() {
		CustomVerifier cv = CustomVerifier.NUMBER_REQ
		assert cv._verify('xxx') == false 
		assert cv._verify('') == false // required field
		assert cv._verify('1.5')
		
		cv = CustomVerifier.NUMBER
		assert cv._verify('xxx') == false
		assert cv._verify('')
		assert cv._verify('1.5')
	}

//	public void testOldVerifier() {
//		CustomVerifier cv = CustomVerifier.NUMBER_REQ
//		assert cv._oldverify('xxx') == false
//		assert cv._oldverify('') == false
//		assert cv._oldverify('1.5')
//		
//		cv = CustomVerifier.NUMBER
//		assert cv._oldverify('xxx') == false // this case fails for old verifier
//		assert cv._oldverify('')
//		assert cv._oldverify('1.5')
//	}
}