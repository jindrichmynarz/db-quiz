// --- jQuery ---

/**
 * @constructor
 * @param {(jQuerySelector|Element|Object|Array.<Element>|jQuery|string|
 *     function())=} arg1
 * @param {(Element|jQuery|Document|
 *     Object.<string, (string|function(!jQuery.event=))>)=} arg2
 * @return {!jQuery}
 */
function jQuery(arg1, arg2) {}

/**
 * @constructor
 * @extends {jQuery}
 * @param {(jQuerySelector|Element|Array.<Element>|Object|jQuery|string|
 *     function())} arg1
 * @param {(Element|jQuery|Document|
 *     Object.<string, (string|function(!jQuery.event=))>)=} arg2
 * @return {!jQuery}
 */
function $(arg1, arg2) {}

// --- Tooltips ---

/** @constructor */
jQuery.tooltip.options = function() {};

/** @type {boolean} */
jQuery.tooltip.prototype.animation;

/** @type {string|function} */
jQuery.tooltip.prototype.placement;

/** @type {string} */
jQuery.tooltip.prototype.selector;

/** @type {string|function} */
jQuery.tooltip.prototype.title;

/** @type {string} */
jQuery.tooltip.prototype.trigger;

/** @type {number|{show: number, hide: number}} */
jQuery.tooltip.prototype.delay;

/**
 * @param {=(string|jQuery.tooltip.options)} opt_eventOrOptions
 * @return {jQuery}
 */
jQuery.prototype.tooltip = function(opt_eventOrOptions) {};
