/*
 * Copyright (c) Josh Reed, 2009.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package psicat.util

import javax.swing.JComponent
import javax.swing.InputVerifier

class CustomVerifier extends InputVerifier {
	static final CustomVerifier NUMBER_REQ = new CustomVerifier(closure: { it.isNumber() })
	static final CustomVerifier NUMBER = new CustomVerifier(required: false, closure: { it.isNumber() })
	
	def closure
	boolean required = true
	boolean verify(JComponent input) { return _verify(input.text) }
	
	public boolean _verify(final String str) {
		if (str != '')
			return closure(str)
		else
			return !required
	}
	
	public boolean _oldverify(final String str) { (!required && str != '') || closure(str) }
}