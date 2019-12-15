// Import the LitElement base class and html helper function
import { LitElement, html, css } from 'lit-element';

// Extend the LitElement base class
export class MyElement extends LitElement {
  static get properties() {
    return {
      id: { type: String },
      name: { type: String },
      min: { type: String },
      max: { type: String },
      value: { type: String }
    };
  }

  constructor() {
    super();
    this.id = ""
    this.name = ""
    this.min = "0";
    this.max = "100";
    this.value = "0";
  }

  handleValueChange(e) {
    var myaddr = this.id;
    /* var slider = this.$.slider; */
    var object = {
              "address": myaddr,
              "data": {
                        value: e.currentTarget.valueAsNumber
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
    return html`
      <!-- template content -->
      <div class="slidecontainer">
        <p><strong>${this.name}</strong></p>
        <input type="range" min="${this.min}" max="${this.max}" value="${this.value}" class="slider" id="myRange" @input="${this.handleValueChange}">
      </div>
    `;
  }
  static get styles() {
    return css`
      .slidecontainer {
        width: 100% /* Width of the outisde container */
      }
      .slider {
        opacity: 0.7;
        width: 100%
        background: #4CAF50; /* green */
      }
      .slider:hover {
        opacity: 1; /* Fully shown on mouse-over */
      }
      p { color: blue; }
    `;
  }

}
// Register the new element with the browser.

customElements.define('arro-slider', MyElement);

