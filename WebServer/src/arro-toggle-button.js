// Import the LitElement base class and html helper function
import { LitElement, html, css } from 'lit-element';

// Extend the LitElement base class
export class MyToggleButton extends LitElement {
  static get properties() {
    return {
      id: { type: String },
      name: { type: String },
      active: { type: String },
      value: {type: String }
      }
    }


  constructor() {
    super();
    this.id = ""
    this.name = ""
    this.active = "1";
    this.value = "";
  }

  getDisabled() {
    return "disabled";
  }

  handleValueChange(e) {
    var myaddr = this.id;
    /* var slider = this.$.slider; */
    var object = {
              "address": myaddr,
              "data": {
                        value: (e.currentTarget.checked ? "on" : "off")
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
      <p><strong>${this.name}</strong></p>
      <div class="slidecontainer">
        <!-- input type="checkbox" checked @input="${this.handleValueChange}"-->
        <input type="checkbox" @input="${this.handleValueChange}" id="switch" ?disabled="${this.value != 0}"/>
        <label for="switch">Toggle</label>
      </div>
    `;
  }
  static get styles() {
    return css`
      /* The switch - the box around the slider */
      .slidecontainer {
        display: flex;
        align-items: center;
        height: 1cm;
      }
      
      input[type=checkbox] {
        height: 0;
        width: 0;
        visibility: hidden;
      }

      label {
        cursor: pointer;
        text-indent: -9999px;
        width: 30px;
        heigth: 10px;
        background: green;
        display: block;
        border-radius: 100px;
        position: relative;
      }

      input:disabled + label {
        background: #dddddd;
      }

      label:after {
        content: "";
        position: absolute;
        top: 3px;
        left: 3px;
        width: 13px;
        height: 12px;
        background: #fff;
        border-radius: 9px;
        transition: 0.3s;
      }

      input:checked + label:after {
        left: calc(100% - 3px);
        transform: translate(-100%);
      }

      label:active:after {
        width: 13px;
      }

      p { color: blue; }
    `;
  }

}
// Register the new element with the browser.

customElements.define('arro-toggle-button', MyToggleButton);

