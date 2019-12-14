// Import the LitElement base class and html helper function
import { LitElement, html, css } from 'lit-element';

// Extend the LitElement base class
export class MyProgress extends LitElement {
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
    return html`
      <!-- template content -->
      <div class="progresscontainer">
        <p><strong>${this.name}</strong></p>
        <progress min="${this.min}" max="${this.max}" value="${this.value}" class="progress" id="myProgress" @input="${this.handleValueChange}">
      </div>
    `;
  }
  static get styles() {
    return css`
      .progresscontainer {
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

customElements.define('arro-progress', MyProgress);

