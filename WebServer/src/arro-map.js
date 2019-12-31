// Import the LitElement base class and html helper function
import { LitElement, svg, css } from 'lit-element';

// Extend the LitElement base class
export class MyMap extends LitElement {
  static get properties() {
    return {
      id: { type: String },
      name: { type: String },
      min: { type: String },
      max: { type: String },
      value: { type: Object }
    };
  }

  constructor() {
    super();
    this.id = ""
    this.name = ""
    this.min = "0";
    this.max = "100";
    this.value = { heading: 0, position: { x: 0, y: 0 } };
  }

  handleValueChange(e) {
    var myaddr = this.id;
    var object = {
              "address": myaddr,
              "data": {
                        value: this.value
                      }
              };
    Window.mySocket.send(JSON.stringify(object) + "\n");
  }

  /**
   * Implement `render` to define a template for your element.
   *
   * You must provide an implementation of `render` for any element
   * that uses LitElement as a base class.
   */
  render(){
    /**
     * `render` must return a lit-html `TemplateResult`.
     *
     * To create a `TemplateResult`, tag a JavaScript template literal
     * with the `html` helper function:
     */
    return svg`
<svg
   xmlns:dc="http://purl.org/dc/elements/1.1/"
   xmlns:cc="http://creativecommons.org/ns#"
   xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
   xmlns:svg="http://www.w3.org/2000/svg"
   xmlns="http://www.w3.org/2000/svg"
   xmlns:sodipodi="http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd"
   xmlns:inkscape="http://www.inkscape.org/namespaces/inkscape"
   sodipodi:docname="background.svg"
   inkscape:version="1.0beta2 (2b71d25, 2019-12-03)"
   id="svg8"
   version="1.1"
   viewBox="0 0 210 297"
   height="297mm"
   width="210mm">
  <defs
     id="defs2" />
  <sodipodi:namedview
     inkscape:window-maximized="0"
     inkscape:window-y="23"
     inkscape:window-x="0"
     inkscape:window-height="712"
     inkscape:window-width="1280"
     showgrid="false"
     inkscape:document-rotation="0"
     inkscape:current-layer="layer1"
     inkscape:document-units="mm"
     inkscape:cy="560"
     inkscape:cx="400"
     inkscape:zoom="0.35"
     inkscape:pageshadow="2"
     inkscape:pageopacity="0.0"
     borderopacity="1.0"
     bordercolor="#666666"
     pagecolor="#ffffff"
     id="base" />
  <metadata
     id="metadata5">
    <rdf:RDF>
      <cc:Work
         rdf:about="">
        <dc:format>image/svg+xml</dc:format>
        <dc:type
           rdf:resource="http://purl.org/dc/dcmitype/StillImage" />
        <dc:title></dc:title>
      </cc:Work>
    </rdf:RDF>
  </metadata>
  <g
     style="display:inline"
     id="layer1"
     inkscape:groupmode="layer"
     inkscape:label="Layer 1">
    <rect
       y="0"
       x="0.75595242"
       height="296.33334"
       width="209.39882"
       id="rect10"
       style="fill:#ffeeaa;stroke-width:0.264583" />
    <circle
       r="0.3775982"
       cy="${this.value.position.y}"
       cx="${this.value.position.x}"
       id="path20"
       style="fill:#000000;stroke:#000000" />
  </g>
</svg>
    `;
  }
  static get styles() {
    return css`
      .mapcontainer {
        width: 100% /* Width of the outisde container */
      }
      .progress {
        opacity: 0.7;
        width: 100%
        background: #4CAF50; /* green */
      }
      .progress:hover {
        opacity: 1; /* Fully shown on mouse-over */
      }
      p { color: blue; }
    `;
  }

}
// Register the new element with the browser.

customElements.define('arro-map', MyMap);

